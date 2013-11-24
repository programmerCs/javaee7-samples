package org.javaee7.batch.listeners;

import org.javaee7.util.BatchTestHelper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Roberto Cortez
 */
@RunWith(Arquillian.class)
public class BatchListenersTest {
    @Inject
    private BatchListenerRecorder batchListenerRecorder;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class)
                                   .addClass(BatchTestHelper.class)
                                   .addPackage("org.javaee7.batch.listeners")
                                   .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                                   .addAsResource("META-INF/batch-jobs/myJob.xml");
        System.out.println(war.toString(true));
        return war;
    }

    @Test
    public void testBatchListeners() throws Exception {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Long executionId = jobOperator.start("myJob", new Properties());
        JobExecution jobExecution = jobOperator.getJobExecution(executionId);

        BatchTestHelper.keepTestAlive(jobExecution);

        List<StepExecution> stepExecutions = jobOperator.getStepExecutions(executionId);
        for (StepExecution stepExecution : stepExecutions) {
            if (stepExecution.getStepName().equals("myStep")) {
                Map<Metric.MetricType, Long> metricsMap = BatchTestHelper.getMetricsMap(stepExecution.getMetrics());

                for (Metric metric : stepExecution.getMetrics()) {
                    System.out.println("metric = " + metric);
                }

                assertEquals(10L, (long) metricsMap.get(Metric.MetricType.READ_COUNT));
                assertEquals(10L / 2L, (long) metricsMap.get(Metric.MetricType.WRITE_COUNT));
                assertEquals(10L / 3 + 10 % 3, (long) metricsMap.get(Metric.MetricType.COMMIT_COUNT));
            }
        }

        assertTrue(batchListenerRecorder.isListenerMethodExecuted(MyJobListener.class, "beforeJob"));
        assertTrue(batchListenerRecorder.isListenerMethodExecuted(MyJobListener.class, "afterJob"));

        assertTrue(batchListenerRecorder.isListenerMethodExecuted(MyStepListener.class, "beforeStep"));
        assertTrue(batchListenerRecorder.isListenerMethodExecuted(MyStepListener.class, "afterStep"));

        assertTrue(batchListenerRecorder.isListenerMethodExecuted(MyChunkListener.class, "beforeChunk"));
        assertTrue(batchListenerRecorder.isListenerMethodExecuted(MyChunkListener.class, "afterChunk"));

        assertTrue(batchListenerRecorder.isListenerMethodExecuted(MyItemReadListener.class, "beforeRead"));
        assertTrue(batchListenerRecorder.isListenerMethodExecuted(MyItemReadListener.class, "afterRead"));
        assertTrue(batchListenerRecorder.isListenerMethodExecuted(MyItemWriteListener.class, "beforeWrite"));
        assertTrue(batchListenerRecorder.isListenerMethodExecuted(MyItemWriteListener.class, "afterWrite"));
        assertTrue(batchListenerRecorder.isListenerMethodExecuted(MyItemProcessorListener.class, "beforeProcess"));
        assertTrue(batchListenerRecorder.isListenerMethodExecuted(MyItemProcessorListener.class, "afterProcess"));

        assertEquals(jobExecution.getBatchStatus(), BatchStatus.COMPLETED);
    }
}
