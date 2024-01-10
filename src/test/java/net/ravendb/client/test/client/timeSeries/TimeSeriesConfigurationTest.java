package net.ravendb.client.test.client.timeSeries;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.timeSeries.*;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.TimeValue;
import net.ravendb.client.primitives.TimeValueUnit;
import net.ravendb.client.serverwide.operations.GetDatabaseRecordOperation;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TimeSeriesConfigurationTest extends RemoteTestBase {

    @Test
    public void testSerialization() throws Exception {
        ObjectMapper mapper = JsonExtensions.getDefaultMapper();

        assertThat(mapper.writeValueAsString(TimeValue.ofHours(2)))
                .isEqualTo("{\"Value\":7200,\"Unit\":\"Second\"}");
    }

    @Test
    public void testDeserialization() throws Exception {
        ObjectMapper mapper = JsonExtensions.getDefaultMapper();

        TimeValue timeValue = mapper.readValue("{\"Value\":7200,\"Unit\":\"Second\"}", TimeValue.class);
        assertThat(timeValue.getUnit())
                .isEqualTo(TimeValueUnit.SECOND);
        assertThat(timeValue.getValue())
                .isEqualTo(7200);

        timeValue = mapper.readValue("{\"Value\":2,\"Unit\":\"Month\"}", TimeValue.class);
        assertThat(timeValue.getUnit())
                .isEqualTo(TimeValueUnit.MONTH);
        assertThat(timeValue.getValue())
                .isEqualTo(2);

        timeValue = mapper.readValue("{\"Value\":0,\"Unit\":\"None\"}", TimeValue.class);
        assertThat(timeValue.getUnit())
                .isEqualTo(TimeValueUnit.NONE);
        assertThat(timeValue.getValue())
                .isEqualTo(0);
    }

    @Test
    @DisabledOnPullRequest
    public void canConfigureTimeSeries() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            TimeSeriesConfiguration config = new TimeSeriesConfiguration();
            store.maintenance().send(new ConfigureTimeSeriesOperation(config));

            config.setCollections(new HashMap<>());
            store.maintenance().send(new ConfigureTimeSeriesOperation(config));

            config.getCollections().put("Users", new TimeSeriesCollectionConfiguration());
            store.maintenance().send(new ConfigureTimeSeriesOperation(config));

            TimeSeriesCollectionConfiguration users = config.getCollections().get("Users");
            users.setPolicies(Arrays.asList(
                    new TimeSeriesPolicy("ByHourFor12Hours", TimeValue.ofHours(1), TimeValue.ofHours(48)),
                    new TimeSeriesPolicy("ByMinuteFor3Hours",TimeValue.ofMinutes(1), TimeValue.ofMinutes(180)),
                    new TimeSeriesPolicy("BySecondFor1Minute",TimeValue.ofSeconds(1), TimeValue.ofSeconds(60)),
                    new TimeSeriesPolicy("ByMonthFor1Year",TimeValue.ofMonths(1), TimeValue.ofYears(1)),
                    new TimeSeriesPolicy("ByYearFor3Years",TimeValue.ofYears(1), TimeValue.ofYears(3)),
                    new TimeSeriesPolicy("ByDayFor1Month",TimeValue.ofDays(1), TimeValue.ofMonths(1))
            ));

            store.maintenance().send(new ConfigureTimeSeriesOperation(config));

            users.setRawPolicy(new RawTimeSeriesPolicy(TimeValue.ofHours(96)));
            store.maintenance().send(new ConfigureTimeSeriesOperation(config));

            TimeSeriesConfiguration updated = store.maintenance().server()
                    .send(new GetDatabaseRecordOperation(store.getDatabase()))
                    .getTimeSeries();

            TimeSeriesCollectionConfiguration collection = updated.getCollections().get("Users");
            List<TimeSeriesPolicy> policies = collection.getPolicies();
            assertThat(policies)
                    .hasSize(6);

            assertThat(policies.get(0).getRetentionTime())
                    .isEqualTo(TimeValue.ofSeconds(60));
            assertThat(policies.get(0).getAggregationTime())
                    .isEqualTo(TimeValue.ofSeconds(1));

            assertThat(policies.get(1).getRetentionTime())
                    .isEqualTo(TimeValue.ofMinutes(180));
            assertThat(policies.get(1).getAggregationTime())
                    .isEqualTo(TimeValue.ofMinutes(1));

            assertThat(policies.get(2).getRetentionTime())
                    .isEqualTo(TimeValue.ofHours(48));
            assertThat(policies.get(2).getAggregationTime())
                    .isEqualTo(TimeValue.ofHours(1));

            assertThat(policies.get(3).getRetentionTime())
                    .isEqualTo(TimeValue.ofMonths(1));
            assertThat(policies.get(3).getAggregationTime())
                    .isEqualTo(TimeValue.ofDays(1));

            assertThat(policies.get(4).getRetentionTime())
                    .isEqualTo(TimeValue.ofYears(1));
            assertThat(policies.get(4).getAggregationTime())
                    .isEqualTo(TimeValue.ofMonths(1));

            assertThat(policies.get(5).getRetentionTime())
                    .isEqualTo(TimeValue.ofYears(3));
            assertThat(policies.get(5).getAggregationTime())
                    .isEqualTo(TimeValue.ofYears(1));
        }
    }
    
    @Test
    public void notValidConfigureShouldThrow() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            final TimeSeriesConfiguration config = new TimeSeriesConfiguration();
            HashMap<String, TimeSeriesCollectionConfiguration> collectionsConfig = new HashMap<>();
            config.setCollections(collectionsConfig);

            TimeSeriesCollectionConfiguration timeSeriesCollectionConfiguration = new TimeSeriesCollectionConfiguration();
            collectionsConfig.put("Users", timeSeriesCollectionConfiguration);

            timeSeriesCollectionConfiguration.setRawPolicy(
                    new RawTimeSeriesPolicy(TimeValue.ofMonths(1)));
            timeSeriesCollectionConfiguration.setPolicies(
                    Collections.singletonList(
                            new TimeSeriesPolicy("By30DaysFor5Years", TimeValue.ofDays(30), TimeValue.ofYears(5))));

            assertThatThrownBy(() -> store.maintenance().send(new ConfigureTimeSeriesOperation(config)))
                    .hasMessageContaining("month might have different number of days");

            final TimeSeriesConfiguration config2 = new TimeSeriesConfiguration();

            collectionsConfig = new HashMap<>();
            config2.setCollections(collectionsConfig);

            timeSeriesCollectionConfiguration = new TimeSeriesCollectionConfiguration();
            collectionsConfig.put("Users", timeSeriesCollectionConfiguration);

            timeSeriesCollectionConfiguration.setRawPolicy(new RawTimeSeriesPolicy(TimeValue.ofMonths(12)));
            timeSeriesCollectionConfiguration.setPolicies(
                    Collections.singletonList(
                            new TimeSeriesPolicy(
                                    "By365DaysFor5Years",
                                    TimeValue.ofSeconds(365 * 24 * 3600),
                                    TimeValue.ofYears(5))));

            assertThatThrownBy(() -> store.maintenance().send(new ConfigureTimeSeriesOperation(config2)))
                    .hasMessageContaining("month might have different number of days");

            final TimeSeriesConfiguration config3 = new TimeSeriesConfiguration();

            collectionsConfig = new HashMap<>();
            config3.setCollections(collectionsConfig);

            timeSeriesCollectionConfiguration = new TimeSeriesCollectionConfiguration();
            collectionsConfig.put("Users", timeSeriesCollectionConfiguration);

            timeSeriesCollectionConfiguration.setRawPolicy(new RawTimeSeriesPolicy(TimeValue.ofMonths(1)));
            timeSeriesCollectionConfiguration.setPolicies(
                    Arrays.asList(
                            new TimeSeriesPolicy("By27DaysFor1Year", TimeValue.ofDays(27), TimeValue.ofYears(1)),
                            new TimeSeriesPolicy("By364DaysFor5Years", TimeValue.ofDays(364), TimeValue.ofYears(5))
                    ));

            assertThatThrownBy(() -> store.maintenance().send(new ConfigureTimeSeriesOperation(config3)))
                    .hasMessageContaining("The aggregation time of the policy 'By364DaysFor5Years' (364 days) must be divided by the aggregation time of 'By27DaysFor1Year' (27 days) without a remainder");
        }
    }

    @Test
    @DisabledOnPullRequest
    public void canExecuteSimpleRollup() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            TimeSeriesPolicy p1 = new TimeSeriesPolicy("BySecond", TimeValue.ofSeconds(1));
            TimeSeriesPolicy p2 = new TimeSeriesPolicy("By2Seconds", TimeValue.ofSeconds(2));
            TimeSeriesPolicy p3 = new TimeSeriesPolicy("By4Seconds", TimeValue.ofSeconds(4));

            TimeSeriesCollectionConfiguration collectionConfig = new TimeSeriesCollectionConfiguration();
            collectionConfig.setPolicies(Arrays.asList(p1, p2, p3));

            TimeSeriesConfiguration config = new TimeSeriesConfiguration();
            config.setCollections(new HashMap<>());
            config.getCollections().put("Users", collectionConfig);

            config.setPolicyCheckFrequency(Duration.ofSeconds(1));

            store.maintenance().send(new ConfigureTimeSeriesOperation(config));

            Date baseLine = DateUtils.addDays(DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH), -1);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Karmel");
                session.store(user, "users/karmel");

                for (int i = 0; i < 100; i++) {
                    session.timeSeriesFor("users/karmel", "Heartrate")
                            .append(DateUtils.addMilliseconds(baseLine, 400 * i), 29.0 * i, "watches/fitbit");
                }

                session.saveChanges();
            }

            // wait for rollups to run
            Thread.sleep(1200);

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> ts = Arrays.asList(session.timeSeriesFor("users/karmel", "Heartrate")
                        .get());

                long tsMillis = ts.get(ts.size() - 1).getTimestamp().getTime() - ts.get(0).getTimestamp().getTime();

                List<TimeSeriesEntry> ts1 = Arrays.asList(session.timeSeriesFor("users/karmel", p1.getTimeSeriesName("Heartrate"))
                        .get());
                long ts1Millis = ts1.get(ts1.size() - 1).getTimestamp().getTime() - ts1.get(0).getTimestamp().getTime();

                assertThat(ts1Millis)
                        .isEqualTo(tsMillis - 600);

                List<TimeSeriesEntry> ts2 = Arrays.asList(session.timeSeriesFor("users/karmel", p2.getTimeSeriesName("Heartrate"))
                        .get());
                assertThat(ts2)
                        .hasSize(ts1.size() / 2);

                List<TimeSeriesEntry> ts3 = Arrays.asList(session.timeSeriesFor("users/karmel", p3.getTimeSeriesName("Heartrate"))
                        .get());
                assertThat(ts3)
                        .hasSize(ts1.size() / 4);
            }
        }
    }

    @Test
    @DisabledOnPullRequest
    public void canConfigureTimeSeries2() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String collectionName = "Users";

            TimeSeriesPolicy p1 = new TimeSeriesPolicy("BySecondFor1Minute", TimeValue.ofSeconds(1), TimeValue.ofSeconds(60));
            TimeSeriesPolicy p2 = new TimeSeriesPolicy("ByMinuteFor3Hours",TimeValue.ofMinutes(1), TimeValue.ofMinutes(180));
            TimeSeriesPolicy p3 = new TimeSeriesPolicy("ByHourFor12Hours",TimeValue.ofHours(1), TimeValue.ofHours(48));
            TimeSeriesPolicy p4 = new TimeSeriesPolicy("ByDayFor1Month",TimeValue.ofDays(1), TimeValue.ofMonths(1));
            TimeSeriesPolicy p5 = new TimeSeriesPolicy("ByMonthFor1Year",TimeValue.ofMonths(1), TimeValue.ofYears(1));
            TimeSeriesPolicy p6 = new TimeSeriesPolicy("ByYearFor3Years",TimeValue.ofYears(1), TimeValue.ofYears(3));

            List<TimeSeriesPolicy> policies = Arrays.asList(p1, p2, p3, p4, p5, p6);

            for (TimeSeriesPolicy policy : policies) {
                store.maintenance().send(new ConfigureTimeSeriesPolicyOperation(collectionName, policy));
            }

            store.maintenance().send(new ConfigureRawTimeSeriesPolicyOperation(collectionName, new RawTimeSeriesPolicy(TimeValue.ofHours(96))));

            ConfigureTimeSeriesValueNamesOperation.Parameters parameters = new ConfigureTimeSeriesValueNamesOperation.Parameters();
            parameters.setCollection(collectionName);
            parameters.setTimeSeries("HeartRate");
            parameters.setValueNames(new String[] { "HeartRate" });
            parameters.setUpdate(true);

            ConfigureTimeSeriesValueNamesOperation nameConfig = new ConfigureTimeSeriesValueNamesOperation(parameters);
            store.maintenance().send(nameConfig);

            TimeSeriesConfiguration updated = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase())).getTimeSeries();
            TimeSeriesCollectionConfiguration collection = updated.getCollections().get(collectionName);
            policies = collection.getPolicies();

            assertThat(policies)
                    .hasSize(6);

            assertThat(policies.get(0).getRetentionTime())
                    .isEqualTo(TimeValue.ofSeconds(60));
            assertThat(policies.get(0).getAggregationTime())
                    .isEqualTo(TimeValue.ofSeconds(1));

            assertThat(policies.get(1).getRetentionTime())
                    .isEqualTo(TimeValue.ofMinutes(180));
            assertThat(policies.get(1).getAggregationTime())
                    .isEqualTo(TimeValue.ofMinutes(1));

            assertThat(policies.get(2).getRetentionTime())
                    .isEqualTo(TimeValue.ofHours(48));
            assertThat(policies.get(2).getAggregationTime())
                    .isEqualTo(TimeValue.ofHours(1));

            assertThat(policies.get(3).getRetentionTime())
                    .isEqualTo(TimeValue.ofMonths(1));
            assertThat(policies.get(3).getAggregationTime())
                    .isEqualTo(TimeValue.ofDays(1));

            assertThat(policies.get(4).getRetentionTime())
                    .isEqualTo(TimeValue.ofYears(1));
            assertThat(policies.get(4).getAggregationTime())
                    .isEqualTo(TimeValue.ofMonths(1));

            assertThat(policies.get(5).getRetentionTime())
                    .isEqualTo(TimeValue.ofYears(3));
            assertThat(policies.get(5).getAggregationTime())
                    .isEqualTo(TimeValue.ofYears(1));

            assertThat(updated.getNamedValues())
                    .isNotNull();

            assertThat(updated.getNamedValues())
                    .hasSize(1);
            String[] mapper = updated.getNames(collectionName, "heartrate");
            assertThat(mapper)
                    .isNotNull()
                    .hasSize(1)
                    .contains("HeartRate");
        }
    }

    @Test
    @DisabledOnPullRequest
    public void canConfigureTimeSeries3() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.timeSeries().setPolicy(User.class, "By15SecondsFor1Minute", TimeValue.ofSeconds(15), TimeValue.ofSeconds(60));
            store.timeSeries().setPolicy(User.class, "ByMinuteFor3Hours", TimeValue.ofMinutes(1), TimeValue.ofMinutes(180));
            store.timeSeries().setPolicy(User.class, "ByHourFor12Hours", TimeValue.ofHours(1), TimeValue.ofHours(48));
            store.timeSeries().setPolicy(User.class, "ByDayFor1Month", TimeValue.ofDays(1), TimeValue.ofMonths(1));
            store.timeSeries().setPolicy(User.class, "ByMonthFor1Year", TimeValue.ofMonths(1), TimeValue.ofYears(1));
            store.timeSeries().setPolicy(User.class, "ByYearFor3Years", TimeValue.ofYears(1), TimeValue.ofYears(3));

            TimeSeriesConfiguration updated = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase())).getTimeSeries();
            TimeSeriesCollectionConfiguration collection = updated.getCollections().get("Users");
            List<TimeSeriesPolicy> policies = collection.getPolicies();

            assertThat(policies)
                    .hasSize(6);

            assertThat(policies.get(0).getRetentionTime())
                    .isEqualTo(TimeValue.ofSeconds(60));
            assertThat(policies.get(0).getAggregationTime())
                    .isEqualTo(TimeValue.ofSeconds(15));

            assertThat(policies.get(1).getRetentionTime())
                    .isEqualTo(TimeValue.ofMinutes(180));
            assertThat(policies.get(1).getAggregationTime())
                    .isEqualTo(TimeValue.ofMinutes(1));

            assertThat(policies.get(2).getRetentionTime())
                    .isEqualTo(TimeValue.ofHours(48));
            assertThat(policies.get(2).getAggregationTime())
                    .isEqualTo(TimeValue.ofHours(1));

            assertThat(policies.get(3).getRetentionTime())
                    .isEqualTo(TimeValue.ofMonths(1));
            assertThat(policies.get(3).getAggregationTime())
                    .isEqualTo(TimeValue.ofDays(1));

            assertThat(policies.get(4).getRetentionTime())
                    .isEqualTo(TimeValue.ofYears(1));
            assertThat(policies.get(4).getAggregationTime())
                    .isEqualTo(TimeValue.ofMonths(1));

            assertThat(policies.get(5).getRetentionTime())
                    .isEqualTo(TimeValue.ofYears(3));
            assertThat(policies.get(5).getAggregationTime())
                    .isEqualTo(TimeValue.ofYears(1));

            assertThatThrownBy(() -> store.timeSeries().removePolicy(User.class, "ByMinuteFor3Hours")).hasMessageContaining("System.InvalidOperationException: The policy 'By15SecondsFor1Minute' has a retention time of '60 seconds' but should be aggregated by policy 'ByHourFor12Hours' with the aggregation time frame of 60 minutes");

            assertThatThrownBy(() -> store.timeSeries().setRawPolicy(User.class, TimeValue.ofSeconds(10))).hasMessageContaining("System.InvalidOperationException: The policy 'rawpolicy' has a retention time of '10 seconds' but should be aggregated by policy 'By15SecondsFor1Minute' with the aggregation time frame of 15 seconds");

            store.timeSeries().setRawPolicy(User.class, TimeValue.ofMinutes(120));
            store.timeSeries().setPolicy(User.class, "By15SecondsFor1Minute", TimeValue.ofSeconds(30), TimeValue.ofSeconds(120));

            updated = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase())).getTimeSeries();
            collection = updated.getCollections().get("Users");
            policies = collection.getPolicies();

            assertThat(policies)
                    .hasSize(6);
            assertThat(policies.get(0).getRetentionTime())
                    .isEqualTo(TimeValue.ofSeconds(120));
            assertThat(policies.get(0).getAggregationTime())
                    .isEqualTo(TimeValue.ofSeconds(30));

            store.timeSeries().removePolicy(User.class, "By15SecondsFor1Minute");

            updated = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase())).getTimeSeries();
            collection = updated.getCollections().get("Users");
            policies = collection.getPolicies();

            assertThat(policies)
                    .hasSize(5);

            store.timeSeries().removePolicy(User.class, RawTimeSeriesPolicy.POLICY_STRING);


        }
    }

}
