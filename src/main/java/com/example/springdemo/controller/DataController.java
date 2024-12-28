package com.example.springdemo.controller;

import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("data")
@ConditionalOnProperty(name="spring.main.web-application-type", havingValue = "!NONE", matchIfMissing = true)
public class DataController {
    private static final ZoneId DB_ZONE = ZoneId.of("America/New_York");

    private NamedParameterJdbcTemplate jdbcTemplate;

    public DataController(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping(path = "xml", produces = {MediaType.APPLICATION_XML_VALUE})
    public String xml() {
        return "<person><id>123</id><firstName>John</firstName><lastName>Doe</lastName></person>";
    }

    @GetMapping(path = "xmlInJson", produces = {MediaType.APPLICATION_JSON_VALUE})
    public String xmlInJson() {
        return "{\"return\": \"" + xml() + "\" }";
    }

    @GetMapping(path = "getStatus")
    public ResponseEntity<?> getStatus(@RequestParam(name = "code", required = false) Optional<Integer> statusCode) {

        Map<String, Object> output = new HashMap<>();
        output.put("status", statusCode.orElse(OK.value()));
        output.put("path", "/data/getStatus");
        return ResponseEntity.status(statusCode.orElse(OK.value())).body(output);
    }

    @GetMapping(path = "loadDates", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> loadDates() {
        String sql = """
                insert into dbo.DATES (id, source, date_time, date_time_2, date_time_2_3)
                values( :id, :source, :dt, :dt, :dt );""";

        List<Instant> instants = List.of(
                OffsetDateTime.parse("2024-03-10T01:59:00.00-05:00", ISO_DATE_TIME).toInstant(),
                OffsetDateTime.parse("2024-11-03T01:59:00.00-04:00", ISO_DATE_TIME).toInstant(),
                Instant.now()
        );
        instants = instants.stream()
                .map(i ->
                        List.of(i, i.plusSeconds(60))
                )
                .flatMap(List::stream)
                .collect(Collectors.toList());

        List<Object> allDates = new ArrayList<>();
        for (Instant now : instants) {
            sql = """
                    insert into dbo.DATES (id, source, date_time, date_time_2, date_time_2_3)
                    values( :id, :source, :dt, :dt, :dt );""";

            OffsetDateTime odtUTC = now.atOffset(ZoneOffset.UTC);
            OffsetDateTime odtEST = now.atZone(DB_ZONE).toOffsetDateTime();
            OffsetDateTime odtDefault = now.atZone(ZoneId.systemDefault()).toOffsetDateTime();
            Date date = Date.from(now);
            Timestamp tstamp = Timestamp.from(now);
            String id = UUID.randomUUID().toString();

            Map<String, Map<String, Object>> mapBefore = Map.of(
                    "LocalDateTime", Map.of("id", id, "source", "LocalDateTime", "dt", odtDefault.toLocalDateTime()),
                    "OffsetDateTime", Map.of("id", id, "source", "OffsetDateTime", "dt", odtDefault),
                    "Date", Map.of("id", id, "source", "Date", "dt", date),
                    "Timestamp", Map.of("id", id, "source", "Timestamp", "dt", tstamp),
                    "Instant", Map.of("id", id, "source", "Instant", "dt", now ) //Timestamp.from(now))
            );

            for (Map<String, Object> m : mapBefore.values()) {
                jdbcTemplate.update(sql, TzAwareSqlParameterSource.of(m));
            }

//            jdbcTemplate.update(sql, TzAwareSqlParameterSource.of(Map.of("id", id, "source", "LocalDateTime", "dt", odtEST.toLocalDateTime())));
//            jdbcTemplate.update(sql, TzAwareSqlParameterSource.of(Map.of("id", id, "source", "OffsetDateTime", "dt", odtUTC)));
//            jdbcTemplate.update(sql, TzAwareSqlParameterSource.of(Map.of("id", id, "source", "Date", "dt", date)));
//            jdbcTemplate.update(sql, TzAwareSqlParameterSource.of(Map.of("id", id, "source", "Timestamp", "dt", tstamp)));
//            jdbcTemplate.update(sql, TzAwareSqlParameterSource.of(Map.of("id", id, "source", "Instant", "dt", Timestamp.from(now))));

            Map<String, DateBean> beanBefore = Map.of(
                    "LocalDateTimeBean", new DateBean(id, "LocalDateTimeBean", odtDefault.toLocalDateTime()),
                    "OffsetDateTimeBean", new DateBean(id, "OffsetDateTimeBean", odtDefault),
                    "DateBean", new DateBean(id, "DateBean", date),
                    "TimestampBean", new DateBean(id, "TimestampBean", tstamp),
                    "InstantBean", new DateBean(id, "InstantBean", now ) //Timestamp.from(now))
            );

            for (DateBean db : beanBefore.values()) {
                jdbcTemplate.update(sql, new TzAwareBeanPropertySqlParameterSource(db));
            }

//            jdbcTemplate.update(sql, new TzAwareBeanPropertySqlParameterSource(new DateBean(id, "LocalDateTimeBean", odtEST.toLocalDateTime())));
//            jdbcTemplate.update(sql, new TzAwareBeanPropertySqlParameterSource(new DateBean(id, "OffsetDateTimeBean", odtUTC)));
//            jdbcTemplate.update(sql, new TzAwareBeanPropertySqlParameterSource(new DateBean(id, "DateBean", date)));
//            jdbcTemplate.update(sql, new TzAwareBeanPropertySqlParameterSource(new DateBean(id, "TimestampBean", tstamp)));
//            jdbcTemplate.update(sql, new TzAwareBeanPropertySqlParameterSource(new DateBean(id, "InstantBean", Timestamp.from(now))));

            Map<String, Map<String, Object>> mapAfter = new HashMap();
            Map<String, DateBean> beanAfter = new HashMap();

            sql = "select TOP 10 ID, source, date_time, date_time_2 from dbo.DATES where ID=? order by source";
            try (Stream<Object> stream = jdbcTemplate.getJdbcTemplate().queryForStream(sql, (rs, roNum) -> {

                String source = rs.getString("source");
                // Timestamp timestamp = Timestamp.from(rs.getTimestamp("date_time").toInstant().atZone(ZoneId.systemDefault()).toInstant());
                Timestamp timestamp = rs.getTimestamp("date_time_2");
                LocalDateTime ldt = rs.getObject("date_time", LocalDateTime.class);
//            OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(timestamp.toInstant(), dbZone).atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime();
                // OffsetDateTime offsetDateTime = ldt.atZone(DB_ZONE).toOffsetDateTime().atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime();
                // OffsetDateTime offsetDateTime = rs.getObject("date_time", OffsetDateTime.class); // SQL Server failure
                // OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(timestamp.toInstant(), DB_ZONE);
                Timestamp timestamp2 = rs.getTimestamp("date_time_2");
                OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(timestamp2.toInstant(),ZoneId.systemDefault()).atZoneSimilarLocal(DB_ZONE).toOffsetDateTime().atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime();
                

                if (offsetDateTime.toInstant().toEpochMilli() != timestamp.getTime()) {
                    System.out.println("differ for " + source + " " + offsetDateTime.toInstant().toEpochMilli() + " != " + timestamp.getTime());
                }

                Object mapValue;
                Object beanValue;
                if (source.contains("Offset")) {
                    mapValue = timestamp.toInstant().atZone(DB_ZONE).toOffsetDateTime();
            //    value = Instant.ofEpochMilli(timestamp.getTime()).atZone(dbZone).toOffsetDateTime();
                    beanValue = offsetDateTime;
                } else if (source.contains("Instant")) {
                    mapValue = timestamp.toInstant();
            //    value = Instant.ofEpochMilli(timestamp.getTime());
                    beanValue = offsetDateTime.toInstant();
                } else if (source.contains("Local")) {
                    mapValue = timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            //    value = Instant.ofEpochMilli(timestamp.getTime()).atZone(dbZone).toLocalDateTime();
                    beanValue = offsetDateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
                } else if (source.contains("stamp")) {
                    mapValue = timestamp;
            //    value = OffsetDateTime.ofInstant(timestamp.toInstant(),dbZone).toInstant();
                    beanValue = Timestamp.from(offsetDateTime.toInstant());
                } else {
                    mapValue = new Date(timestamp.getTime());
            //    value = new Date(offsetDateTime.toInstant().toEpochMilli());
                    beanValue = Date.from(offsetDateTime.toInstant());
                }

                if (source.endsWith("Bean")) {
                    DateBean dateBean = new DateBean(id, source, beanValue);
                    beanAfter.put(source, dateBean);
                    return dateBean;
                } else {
                    Map dateMap = Map.of("id", id, "source", source, "dt", mapValue);
                    mapAfter.put(source, dateMap);
                    return dateMap;
                }
            }, id)) {
//                return ResponseEntity.ok(stream.collect(Collectors.toList()));
//                allDates.addAll(stream.collect(Collectors.toList()));
                stream.collect(Collectors.toList());
                allDates.addAll(mapBefore.keySet().stream().map(s -> {
                            Map<String, Object> mb = mapBefore.get(s);
                            DateBean bb = beanBefore.get(s + "Bean");
                            Map<String, Object> ma = mapAfter.get(s);
                            DateBean ba = beanAfter.get(s + "Bean");

                            return InstantResult.builder()
                                    .instantString(formatDate(now))
                                    .instantMillis(now.toEpochMilli())
                                    .source(s)
                                    .mapBeforeString(formatDate(mb))
                                    .mapBeforeMillis(formatEpoch(mb, now))
                                    .beanBeforeString(formatDate(bb))
                                    .beanBeforeMillis(formatEpoch(bb, now))
                                    .mapAfterString(formatDate(ma))
                                    .mapAfterMillis(formatEpoch(ma, now))
                                    .beanAfterString(formatDate(ba))
                                    .beanAfterMillis(formatEpoch(ba, now))
                                    .build();
                        })
                        .map(ir -> ir.toDelimitedString(","))
                        .collect(Collectors.toList()));
                ;
            }
        }

        return ResponseEntity.ok(allDates.stream()
                .map(d -> d.toString())
                .collect(Collectors.joining("\n")));
    }

    DateTimeFormatter DTF = ISO_DATE_TIME.withZone(ZoneId.systemDefault());

    private String formatDate(Object obj) {
        Object dt;
        if (obj instanceof DateBean) {
            dt = ((DateBean) obj).getDt();
        } else if (obj instanceof Map) {
            dt = ((Map) obj).get("dt");
        } else {
            dt = obj;
        }

        if (dt instanceof Instant) {
            return DTF.format((Instant) dt);
        } else if (dt instanceof OffsetDateTime) {
            return DTF.format((OffsetDateTime) dt);
        } else if (dt instanceof Date) {
            return DTF.format(((Date) dt).toInstant());
        } else if (dt instanceof LocalDateTime) {
            return DTF.format((LocalDateTime) dt);
        } else if (dt instanceof Timestamp) {
            return DTF.format(((Timestamp) dt).toInstant());
        } else {
            throw new IllegalArgumentException("unable to format " + obj.getClass().getSimpleName());
        }
    }

    private long formatEpoch(Object obj, Instant now) {
        Object dt;
        if (obj instanceof DateBean) {
            dt = ((DateBean) obj).getDt();
        } else if (obj instanceof Map) {
            dt = ((Map) obj).get("dt");
        } else {
            dt = obj;
        }

        if (dt instanceof Instant) {
            return ((Instant) dt).toEpochMilli();
        } else if (dt instanceof OffsetDateTime) {
            return ((OffsetDateTime) dt).toInstant().toEpochMilli();
        } else if (dt instanceof Date) {
            return ((Date) dt).getTime();
        } else if (dt instanceof LocalDateTime) {
            // return ((LocalDateTime) dt).atOffset(ZoneId.systemDefault().getRules().getOffset(now)).toInstant().toEpochMilli();
            return ((LocalDateTime) dt).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } else if (dt instanceof Timestamp) {
            // return ((Timestamp) dt).toInstant().toEpochMilli();
            return ((Timestamp) dt).getTime();
        } else {
            throw new IllegalArgumentException("unable to format " + obj.getClass().getSimpleName());
        }
    }

    private static class DateBean {
        private final String id;
        private String source;
        private Object dt;

        public DateBean(String id, String source, Object dt) {
            this.id = id;
            this.source = source;
            this.dt = dt;
        }

        public String getId() {
            return id;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public Object getDt() {
            return dt;
        }

        public void setDt(Object dt) {
            this.dt = dt;
        }
    }

    @Data
    @Builder
    public static class InstantResult {
        private String instantString;
        private long instantMillis;
        private String source;
        private String mapBeforeString;
        private long mapBeforeMillis;
        private String mapAfterString;
        private long mapAfterMillis;
        private String beanBeforeString;
        private long beanBeforeMillis;
        private String beanAfterString;
        private long beanAfterMillis;

        public String toDelimitedString(String delimiter) {
            return new StringBuilder()
                    .append(instantString).append(delimiter)
                    .append(instantMillis).append(delimiter)
                    .append(source).append(delimiter)
                    .append(mapBeforeString).append(delimiter)
                    .append(mapBeforeMillis).append(delimiter)
                    .append(mapAfterString).append(delimiter)
                    .append(mapAfterMillis).append(delimiter)
                    .append(beanBeforeString).append(delimiter)
                    .append(beanBeforeMillis).append(delimiter)
                    .append(beanAfterString).append(delimiter)
                    .append(beanAfterMillis).append(delimiter)
                    .toString();
        }
    }

    private static class TzAwareSqlParameterSource extends MapSqlParameterSource {

        public static final ZoneId DB_ZONE = ZoneId.of("America/New_York");

        public TzAwareSqlParameterSource() {
            super();
        }

        public TzAwareSqlParameterSource(String paramName, Object value) {
            super(paramName, value);
        }

        public TzAwareSqlParameterSource(Map<String, ?> values) {
            super(values);
        }

        @Override
        public Object getValue(@NotNull String paramName) {
            Object result = super.getValue(paramName);
            if (result instanceof OffsetDateTime) {
                return ((OffsetDateTime) result).atZoneSameInstant(DB_ZONE).toOffsetDateTime();
            } else if (result instanceof LocalDateTime) {
                // return ((LocalDateTime) result).atZone(ZoneId.systemDefault()).withZoneSameInstant(DB_ZONE).toLocalDateTime();
                return ((LocalDateTime) result).atZone(ZoneId.systemDefault()).withZoneSameInstant(DB_ZONE).toOffsetDateTime();
            } else if (result instanceof Instant) {
                return ((Instant) result).atZone(DB_ZONE).toOffsetDateTime();
            } else if (result instanceof Timestamp) {
                // return ((Timestamp) result).toInstant().atZone(DB_ZONE).toOffsetDateTime();
                return result;
            } else if (result instanceof Date) {
                return ((Date) result).toInstant().atZone(DB_ZONE).toOffsetDateTime();
            } else {
                return result;
            }
        }

        public static TzAwareSqlParameterSource of(@Nullable Map<String, ?> values) {
            return new TzAwareSqlParameterSource(values);
        }
    }

    public static class TzAwareBeanPropertySqlParameterSource extends BeanPropertySqlParameterSource {
        public static final ZoneId DB_ZONE = ZoneId.of("America/New_York");

        public TzAwareBeanPropertySqlParameterSource(Object object) {
            super(object);
        }

        @Override
        public Object getValue(String paramName) throws IllegalArgumentException {
            Object result = super.getValue(paramName);
            if (result instanceof OffsetDateTime) {
                return ((OffsetDateTime) result).atZoneSameInstant(DB_ZONE).toOffsetDateTime();
            } else if (result instanceof LocalDateTime) {
                // return ((LocalDateTime) result).atZone(ZoneId.systemDefault()).toOffsetDateTime().atZoneSameInstant(DB_ZONE).toLocalDateTime();
                return ((LocalDateTime) result).atZone(ZoneId.systemDefault()).toOffsetDateTime().atZoneSameInstant(DB_ZONE).toOffsetDateTime();
            } else if (result instanceof Instant) {
                return ((Instant) result).atZone(DB_ZONE).toOffsetDateTime();
            } else if (result instanceof Timestamp) {
                return ((Timestamp) result).toInstant().atZone(DB_ZONE).toOffsetDateTime();
            } else if (result instanceof Date) {
                return ((Date) result).toInstant().atZone(DB_ZONE).toOffsetDateTime();
            } else {
                return result;
            }
        }
    }
}
