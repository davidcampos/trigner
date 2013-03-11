package pt.ua.tm.trigner.dictionary;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.util.FileUtil;
import pt.ua.tm.neji.context.Context;
import pt.ua.tm.neji.core.batch.Batch;
import pt.ua.tm.neji.core.corpus.InputCorpus;
import pt.ua.tm.neji.core.corpus.OutputCorpus;
import pt.ua.tm.neji.core.processor.Processor;
import pt.ua.tm.neji.exception.NejiException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 20/12/12
 * Time: 14:59
 * To change this template use File | Settings | File Templates.
 */
public class FolderBatch implements Batch {

    private static Logger logger = LoggerFactory.getLogger(FolderBatch.class);
    private Collection<Corpus> processedCorpora;
    private String inputFolderPath, outputFolderPath;
    private int numThreads;

    public FolderBatch(final String inputFolderPath, final String outputFolderPath, final int numThreads) {
        this.inputFolderPath = inputFolderPath;
        this.outputFolderPath = outputFolderPath;
        this.numThreads = numThreads;
        this.processedCorpora = new ArrayList<>();
    }

    @Override
    public void run(Class<Processor> processorClass, Context context, Object... objects) throws NejiException {
        throw new NotImplementedException("Not implemented. Use void run(Context context) instead.");
    }

    public void run(final Context context) throws NejiException {
        logger.info("Initializing context...");
        context.initialize();
        logger.info("Installing multi-threading support...");
        context.addMultiThreadingSupport(numThreads);

        ExecutorService executor;

        logger.info("Starting thread pool with support for {} threads...", numThreads);
        executor = Executors.newFixedThreadPool(numThreads);

        StopWatch timer = new StopWatch();
        timer.start();

        File inputFolder = new File(inputFolderPath);
        File[] files = inputFolder.listFiles(new FileUtil.Filter(new String[]{"txt"}));

        for (File file : files) {
            File outputFile = OutputCorpus.newOutputFile(outputFolderPath, file.getName().replaceAll(".txt", ""), OutputCorpus.OutputFormat.A1, false);
            Processor processor = getDocumentProcessor(file, outputFile, context);

            // Process entry
            executor.execute(processor);
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        logger.info("Stopped thread pool.");

        logger.info("Terminating context...");
        context.terminate();

        timer.stop();
        logger.info("Processed {} files in {}", processedCorpora.size(), timer.toString());
    }

    @Override
    public Collection<Corpus> getProcessedCorpora() {
        return processedCorpora;
    }

    private Processor getDocumentProcessor(final File inputTextFile, final File outputFile, Context context) {
        Corpus corpus = new Corpus();
        Processor processor;

        try {
            InputCorpus textCorpus = new InputCorpus(inputTextFile, InputCorpus.InputFormat.RAW, false, corpus);
            OutputCorpus outputCorpus = new OutputCorpus(outputFile, OutputCorpus.OutputFormat.A1, false, corpus);

            processedCorpora.add(corpus);

            processor = new DocumentProcessor(context, textCorpus, outputCorpus);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return processor;
    }
}
