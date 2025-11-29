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
                     LIMIT ?
                       FOR UPDATE SKIP LOCKED
                     """);
            selectPs.setInt(1, limit);
            ResultSet rs = selectPs.executeQuery();

            ArrayList<Integer> ids = new java.util.ArrayList<>();
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
                        UPDATE workqueue
                           SET status = 'I',
                               update_dt = CURRENT_TIMESTAMP,
                               msg = ?
                         WHERE id IN (""" + inClause + ")");
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
                      FROM workqueue
                     WHERE id IN (""" + inClause + ")");
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

    public static ResultSet outputNext(Connection conn, String msg) throws SQLException {
        return selectNext(conn, msg);
    }

    public static ResultSet outputMany(Connection conn, int limit, String msg) throws SQLException {
        return selectMany(conn, limit, msg);
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
