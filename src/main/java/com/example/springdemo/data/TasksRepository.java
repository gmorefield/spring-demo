package com.example.springdemo.data;

import com.example.springdemo.model.TriggerSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;

@Component
@Slf4j
public class TasksRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TasksRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TriggerSpec> findAll() {
        return jdbcTemplate.query("select * from trigger_spec", (resultSet, i) -> toSpec(resultSet));
    }

    private TriggerSpec toSpec(ResultSet resultSet) throws SQLException {
        return TriggerSpec.builder()
                .id(resultSet.getInt("id"))
                .cronExpression(resultSet.getString("cron_expression"))
                .initialDelay(Duration.parse(resultSet.getString("initial_delay")))
                .interval(Duration.parse(resultSet.getString("interval")))
                .targetBean(resultSet.getString("target_bean"))
                .targetMethod(resultSet.getString("target_method"))
                .triggerType(TriggerSpec.TriggerType.valueOf(resultSet.getString("trigger_type")))
                .enabled(resultSet.getBoolean("enabled"))
                .build();
    }
}
