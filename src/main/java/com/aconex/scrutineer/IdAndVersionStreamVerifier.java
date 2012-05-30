package com.aconex.scrutineer;

import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

import com.aconex.scrutineer.javautil.StringIdAndVersionComparator;

public class IdAndVersionStreamVerifier {

    private static final Logger LOG = LogUtils.loggerForThisClass();

    private final ExecutorService executorService = Executors.newFixedThreadPool(2, new NamedDaemonThreadFactory("StreamOpener"));
	private final Comparator<IdAndVersion> comparator;

	public IdAndVersionStreamVerifier() {
		this (new StringIdAndVersionComparator());
	}

	public IdAndVersionStreamVerifier(Comparator<IdAndVersion> comparator) {
		this.comparator = comparator;
	}

    //CHECKSTYLE:OFF
    @SuppressWarnings("PMD.NcssMethodCount")
	public void verify(IdAndVersionStream primaryStream, IdAndVersionStream secondayStream, IdAndVersionStreamVerifierListener idAndVersionStreamVerifierListener) {
        long numItems = 0;
        long begin = System.currentTimeMillis();

        try {

            parallelOpenStreamsAndWait(primaryStream, secondayStream);

            Iterator<IdAndVersion> primaryIterator = primaryStream.iterator();
            Iterator<IdAndVersion> secondaryIterator = secondayStream.iterator();

            IdAndVersion primaryItem = next(primaryIterator);
            IdAndVersion secondaryItem = next(secondaryIterator);

            while (primaryItem != null && secondaryItem != null) {
                if (primaryItem.equals(secondaryItem)) {
                    primaryItem = next(primaryIterator);
                    secondaryItem = next(secondaryIterator);
                } else if (primaryItem.getId().equals(secondaryItem.getId())) {
                    idAndVersionStreamVerifierListener.onVersionMisMatch(primaryItem, secondaryItem);
                    primaryItem = next(primaryIterator);
                    secondaryItem = next(secondaryIterator);
                } else if (comparator.compare(primaryItem, secondaryItem) < 0) {
                    idAndVersionStreamVerifierListener.onMissingInSecondaryStream(primaryItem);
                    primaryItem = next(primaryIterator);
                } else {
                    idAndVersionStreamVerifierListener.onMissingInPrimaryStream(secondaryItem);
                    secondaryItem = next(secondaryIterator);
                }
                numItems++;
            }

            while (primaryItem != null) {
                idAndVersionStreamVerifierListener.onMissingInSecondaryStream(primaryItem);
                primaryItem = next(primaryIterator);
                numItems++;
            }

            while (secondaryItem != null) {
                idAndVersionStreamVerifierListener.onMissingInPrimaryStream(secondaryItem);
                secondaryItem = next(secondaryIterator);
                numItems++;
            }
        } finally {
            closeWithoutThrowingException(primaryStream);
            closeWithoutThrowingException(secondayStream);
        }
        LogUtils.infoTimeTaken(LOG, begin, numItems, "Completed verification");
    }
    //CHECKSTYLE:ON

    private void parallelOpenStreamsAndWait(IdAndVersionStream primaryStream, IdAndVersionStream secondaryStream) {
        Future<?> primaryOpenCall = executorService.submit(new OpenStreamRunner(primaryStream));
        Future<?> secondaryOpenCall = executorService.submit(new OpenStreamRunner(secondaryStream));

        getAllFutures(primaryOpenCall, secondaryOpenCall);
    }

    private void getAllFutures(Future<?>... futures) {
        try {
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to open one or both of the streams in parallel", e);
        }
    }

    private IdAndVersion next(Iterator<IdAndVersion> iterator) {
        if (iterator.hasNext()) {
            return iterator.next();
        } else {
            return null;
        }
    }

    private void closeWithoutThrowingException(IdAndVersionStream idAndVersionStream) {
        try {
            idAndVersionStream.close();
        } catch (Exception e) {
            LogUtils.warn(LOG, "Unable to close IdAndVersionStream", e);
        }
    }

    private static class OpenStreamRunner implements Runnable {
        private final IdAndVersionStream primaryStream;

        public OpenStreamRunner(IdAndVersionStream primaryStream) {
            this.primaryStream = primaryStream;
        }

        @Override
        public void run() {
            primaryStream.open();
        }
    }

    private static class NamedDaemonThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger threadCount = new AtomicInteger();

        public NamedDaemonThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable command) {
            Thread thread = new Thread(command, namePrefix + "-" + threadCount.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
