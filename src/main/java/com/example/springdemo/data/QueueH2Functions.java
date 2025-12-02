package com.example.springdemo.data;

import org.h2.tools.SimpleResultSet;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

public class QueueH2Functions {

    public static ResultSet selectNext(Connection conn, String msg) throws SQLException {
        String url = conn.getMetaData().getURL();
        if (url.equals("jdbc:columnlist:connection")) {
            return getMetaResultSet();
        }

        try {
            conn.setAutoCommit(false);

            PreparedStatement selectPs = conn.prepareStatement("""
                    SELECT id
                      FROM workqueue
                     WHERE status = 'R'
                     ORDER BY id
                     LIMIT 1
                       FOR UPDATE SKIP LOCKED
                     """);
            ResultSet rs = selectPs.executeQuery();

            int id = rs.next() ? rs.getInt("id") : -1;
            if (id != -1) {
                PreparedStatement updatePs = conn.prepareStatement("""
                        UPDATE workqueue
                           SET status = 'I',
                               update_dt = CURRENT_TIMESTAMP,
                               msg = ?
                         WHERE id = ?""");
                updatePs.setString(1, msg);
                updatePs.setInt(2, id);
                updatePs.executeUpdate();
            }

            conn.commit();
            conn.setAutoCommit(true);

            PreparedStatement ps = conn.prepareStatement("""
                    SELECT *
                      FROM workqueue
                     WHERE id = ?""");
            ps.setInt(1, id);
            return ps.executeQuery();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
            throw e;
        }
    }

    public static ResultSet selectMany(Connection conn, int limit, String msg) throws SQLException {
        return selectManyWork(conn, limit, msg);
    }

    public static ResultSet outputNext(Connection conn, String msg) throws SQLException {
        return selectNext(conn, msg);
    }

    public static ResultSet outputMany(Connection conn, int limit, String msg) throws SQLException {
        return selectManyWork(conn, limit, msg);
    }

    // NOTE: This one doesn't work properly for more limit of 1 since it can return multiple rows per order_id
    public static ResultSet outputManyOrderedNotExists(Connection conn, int limit, String msg) throws SQLException {
        String tableName = "orderedqueue";
        String query = """
                SELECT id
                  FROM %1$s o
                 WHERE o.status = 'R'
                   AND NOT EXISTS (
                       SELECT 1
                         FROM %1$s s
                        WHERE s.order_id = o.order_id
                          AND s.status NOT IN ('C', 'R')
                   )
                 ORDER BY o.id
                 LIMIT ?
                   FOR UPDATE
                 """.formatted(tableName);

        return selectMany(conn, query, tableName, limit, msg);
    }

    public static ResultSet outputManyOrderedPart(Connection conn, int limit, String msg) throws SQLException {
        String tableName = "orderedqueue";
        String query = """
                SELECT o.id
                  FROM (
                       SELECT row_number() OVER (PARTITION BY order_id ORDER BY id) as rn, id, status
                         FROM %1$s i
                        WHERE i.status != 'C'
                  ) AS o
                 WHERE o.rn = 1
                   AND o.status = 'R'
                 ORDER BY o.id
                 LIMIT ?
                   FOR UPDATE
                 """.formatted(tableName);

        return selectMany(conn, query, tableName, limit, msg);
    }

    public static ResultSet outputManyOrderedCross(Connection conn, int limit, String msg) throws SQLException {
        return outputManyOrderedSub(conn, limit, msg);
    }

    public static ResultSet outputManyOrderedSub(Connection conn, int limit, String msg) throws SQLException {
        String tableName = "orderedqueue";
        String query = """
                SELECT o.id
                  FROM %1s o
                 WHERE o.status = 'R'
                   AND o.id = (
                       SELECT MIN(i.id)
                         FROM %1$s i
                        WHERE i.order_id = o.order_id
                          AND i.status != 'C'
                   )
                 ORDER BY o.id
                 LIMIT ?
                   FOR UPDATE
                 """.formatted(tableName);

        return selectMany(conn, query, tableName, limit, msg);
    }

    // ------------------------------------------------------------------------
    // Helper methods
    // ------------------------------------------------------------------------

    private static ResultSet selectManyWork(Connection conn, int limit, String msg) throws SQLException {
        String tableName = "workqueue";
        String query = """
                SELECT id
                  FROM %1$s
                 WHERE status = 'R'
                 ORDER BY id
                 LIMIT ?
                   FOR UPDATE SKIP LOCKED
                 """.formatted(tableName);
        return selectMany(conn, query, tableName, limit, msg);
    }

    private static ResultSet selectMany(Connection conn, String query, String tableName, int limit, String msg) throws SQLException {
        String url = conn.getMetaData().getURL();
        if (url.equals("jdbc:columnlist:connection")) {
            return getMetaResultSet();
        }

        try {
            conn.setAutoCommit(false);

            PreparedStatement selectPs = conn.prepareStatement(query);
            selectPs.setInt(1, limit);
            ResultSet rs = selectPs.executeQuery();

            ArrayList<Integer> ids = new ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getInt("id"));
            }

            if (ids.isEmpty()) {
                ids.add(-1);
            }

            StringBuilder inClause = new StringBuilder();
            for (int i = 0; i < ids.size(); i++) {
                if (i > 0) inClause.append(",");
                inClause.append("?");
            }

            if (ids.get(0) != -1) {
                PreparedStatement updatePs = conn.prepareStatement("""
                        UPDATE %1$s
                           SET status = 'I',
                               update_dt = CURRENT_TIMESTAMP,
                               msg = ?
                         WHERE id IN (%2$s)""".formatted(tableName, inClause.toString()));
                updatePs.setString(1, msg);
                for (int i = 0; i < ids.size(); i++) {
                    updatePs.setInt(i + 2, ids.get(i));
                }
                updatePs.executeUpdate();
            }

            conn.commit();
            conn.setAutoCommit(true);

            PreparedStatement ps = conn.prepareStatement("""
                    SELECT *
                      FROM %1$s
                     WHERE id IN (%2$s)""".formatted(tableName, inClause.toString()));
            for (int i = 0; i < ids.size(); i++) {
                ps.setInt(i + 1, ids.get(i));
            }
            return ps.executeQuery();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
            throw e;
//        } finally {
//            conn.setAutoCommit(true);
        }
    }

    @NotNull
    private static SimpleResultSet getMetaResultSet() {
        SimpleResultSet rs = new SimpleResultSet();
        rs.addColumn("wid", Types.VARCHAR, 36, 0);
        rs.addColumn("status", Types.VARCHAR, 1, 0);
        rs.addColumn("retry_cnt", Types.INTEGER, 10, 0);
        rs.addColumn("msg", Types.VARCHAR, 25, 0);
        rs.addColumn("create_dt", Types.TIMESTAMP, 23, 3);
        rs.addColumn("update_dt", Types.TIMESTAMP, 23, 3);
        rs.addColumn("id", Types.INTEGER, 10, 0);
        return rs;
    }
}
