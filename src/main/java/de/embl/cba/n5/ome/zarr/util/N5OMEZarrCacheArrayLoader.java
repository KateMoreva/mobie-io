package de.embl.cba.n5.ome.zarr.util;

import bdv.img.cache.SimpleCacheArrayLoader;
import com.amazonaws.SdkClientException;
import de.embl.cba.n5.ome.zarr.loaders.N5OMEZarrImageLoader;
import net.imglib2.img.cell.CellGrid;
import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;

import java.io.IOException;
import java.util.Arrays;

import static de.embl.cba.n5.ome.zarr.loaders.N5OMEZarrImageLoader.logChunkLoading;

public class N5OMEZarrCacheArrayLoader<A> implements SimpleCacheArrayLoader<A> {
    private final N5Reader n5;
    private final String pathName;
    private final int channel;
    private final int timepoint;
    private final DatasetAttributes attributes;
    private final ArrayCreator<A, ?> arrayCreator;
    private final ZarrAxes zarrAxes;

    public N5OMEZarrCacheArrayLoader(final N5Reader n5, final String pathName, final int channel, final int timepoint, final DatasetAttributes attributes, CellGrid grid, ZarrAxes zarrAxes) {
        this.n5 = n5;
        this.pathName = pathName; // includes the level
        this.channel = channel;
        this.timepoint = timepoint;
        this.attributes = attributes;
        this.arrayCreator = new ArrayCreator<>(grid, attributes.getDataType(), zarrAxes);
        this.zarrAxes = zarrAxes;
    }

    @Override
    public A loadArray(final long[] gridPosition) throws IOException {
        DataBlock<?> block = null;

        long[] dataBlockIndices = toDataBlockIndices(gridPosition);

        long start = 0;
        if (logChunkLoading) {
            start = System.currentTimeMillis();
            System.out.println(pathName + " " + Arrays.toString(dataBlockIndices) + " ...");
        }

        try {
//                System.out.println("dataBlockIndices" + Arrays.toString(dataBlockIndices));
            block = n5.readBlock(pathName, attributes, dataBlockIndices);
        } catch (SdkClientException e) {
            System.err.println(e); // this happens sometimes, not sure yet why...
        }

        if (logChunkLoading) {
            if (block != null)
                System.out.println(pathName + " " + Arrays.toString(dataBlockIndices) + " fetched " + block.getNumElements() + " voxels in " + (System.currentTimeMillis() - start) + " ms.");
            else
                System.out.println(pathName + " " + Arrays.toString(dataBlockIndices) + " is missing, returning zeros.");
        }

        if (block == null) {
            return arrayCreator.createEmptyArray(gridPosition);
        } else {
            return arrayCreator.createArray(block, gridPosition);
        }
    }

    private long[] toDataBlockIndices(long[] gridPosition) {
        long[] dataBlockIndices = gridPosition;

        if (zarrAxes.is2D()) {
            dataBlockIndices = new long[2];
            System.arraycopy(gridPosition, 0, dataBlockIndices, 0, 2);
        }

        if (zarrAxes.is4DWithTimepointsAndChannels()) {
            dataBlockIndices = new long[4];
            System.arraycopy(gridPosition, 0, dataBlockIndices, 0, 2);
            dataBlockIndices[2] = channel;
            dataBlockIndices[3] = timepoint;
        }

        if (zarrAxes.is5D()) {
            dataBlockIndices = new long[5];
            System.arraycopy(gridPosition, 0, dataBlockIndices, 0, 3);
            dataBlockIndices[3] = channel;
            dataBlockIndices[4] = timepoint;
        }

        if (zarrAxes.is4DWithChannels()) {
            dataBlockIndices = new long[4];
            System.arraycopy(gridPosition, 0, dataBlockIndices, 0, 3);
            dataBlockIndices[3] = channel;
        }

        if (zarrAxes.is4DWithTimepoints()) {
            dataBlockIndices = new long[4];
            System.arraycopy(gridPosition, 0, dataBlockIndices, 0, 3);
            dataBlockIndices[3] = timepoint;
        }

        if (dataBlockIndices == null)
            throw new RuntimeException("Could not determine the data block to be loaded.");

        return dataBlockIndices;
    }
}