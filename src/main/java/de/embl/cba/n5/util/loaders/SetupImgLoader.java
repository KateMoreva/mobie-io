//package de.embl.cba.n5.util.loaders;
//
//import bdv.AbstractViewerSetupImgLoader;
//import bdv.img.cache.SimpleCacheArrayLoader;
//import bdv.util.ConstantRandomAccessible;
//import bdv.util.MipmapTransforms;
//import de.embl.cba.n5.ome.zarr.util.N5OMEZarrCacheArrayLoader;
//import de.embl.cba.n5.ome.zarr.util.OmeZarrMultiscales;
//import de.embl.cba.n5.ome.zarr.util.ZarrAxes;
//import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
//import mpicbg.spim.data.sequence.MultiResolutionSetupImgLoader;
//import mpicbg.spim.data.sequence.VoxelDimensions;
//import net.imglib2.*;
//import net.imglib2.cache.volatiles.CacheHints;
//import net.imglib2.cache.volatiles.LoadingStrategy;
//import net.imglib2.img.cell.CellGrid;
//import net.imglib2.img.cell.CellImg;
//import net.imglib2.realtransform.AffineTransform3D;
//import net.imglib2.type.NativeType;
//import net.imglib2.view.Views;
//import org.janelia.saalfeldlab.n5.DatasetAttributes;
//import org.janelia.saalfeldlab.n5.N5Reader;
//import org.jetbrains.annotations.NotNull;
//
//import java.io.IOException;
//import java.util.Arrays;
//
//public class SetupImgLoader<T extends NativeType<T>, V extends Volatile<T> & NativeType<V>>
//        extends AbstractViewerSetupImgLoader<T, V>
//        implements MultiResolutionSetupImgLoader<T> {
//    private final int setupId;
//
//    private final double[][] mipmapResolutions;
//
//    private final AffineTransform3D[] mipmapTransforms;
//
//    public SetupImgLoader(final int setupId, final T type, final V volatileType) throws IOException {
//        super(type, volatileType);
//        this.setupId = setupId;
//        mipmapResolutions = readMipmapResolutions();
//        mipmapTransforms = new AffineTransform3D[mipmapResolutions.length];
//        for (int level = 0; level < mipmapResolutions.length; level++) {
//            mipmapTransforms[level] = MipmapTransforms.getMipmapTransformDefault(mipmapResolutions[level]);
//        }
//    }
//
//    /**
//     * @return
//     * @throws IOException
//     */
//    private double[][] readMipmapResolutions() throws IOException {
//        OmeZarrMultiscales multiscale = setupToMultiscale.get(setupId);
//        double[][] mipmapResolutions = new double[multiscale.datasets.length][];
//
////Try to fix 2D problem
//        long[] dimensionsOfLevel0 = getDatasetAttributes(getPathName(setupId, 0)).getDimensions();
//        mipmapResolutions[0] = new double[]{1.0, 1.0, 1.0};
//
//        //Try to fix 2D problem
//        for (int level = 1; level < mipmapResolutions.length; level++) {
//            long[] dimensions = getDatasetAttributes(getPathName(setupId, level)).getDimensions();
//            mipmapResolutions[level] = new double[3];
//            if (dimensions.length < 3 && dimensionsOfLevel0.length < 3) {
//                for (int d = 0; d < 2; d++) {
//                    mipmapResolutions[level][d] = 1.0 * dimensionsOfLevel0[d] / dimensions[d];
//                }
//                mipmapResolutions[level][2] = 1.0;
//            } else {
//                for (int d = 0; d < 3; d++) {
//                    mipmapResolutions[level][d] = 1.0 * dimensionsOfLevel0[d] / dimensions[d];
//                }
//            }
//        }
//
//        return mipmapResolutions;
//    }
//
//    @Override
//    public RandomAccessibleInterval<V> getVolatileImage(final int timepointId, final int level, final ImgLoaderHint... hints) {
//        return prepareCachedImage(timepointId, level, LoadingStrategy.BUDGETED, volatileType);
//    }
//
//    @Override
//    public RandomAccessibleInterval<T> getImage(final int timepointId, final int level, final ImgLoaderHint... hints) {
//        return prepareCachedImage(timepointId, level, LoadingStrategy.BLOCKING, type);
//    }
//
//    @Override
//    public Dimensions getImageSize(final int timepointId, final int level) {
//        final String pathName = getPathName(setupId, level);
//        try {
//            final DatasetAttributes attributes = getDatasetAttributes(pathName);
//            return new FinalDimensions(attributes.getDimensions());
//        } catch (Exception e) {
//            throw new RuntimeException("Could not read from " + pathName);
//        }
//    }
//
//    @NotNull
//    public String getPathName(int setupId, int level) {
//        return setupToPathname.get(setupId) + "/" + setupToMultiscale.get(this.setupId).datasets[level].path;
//    }
//
//    @Override
//    public double[][] getMipmapResolutions() {
//        return mipmapResolutions;
//    }
//
//    @Override
//    public AffineTransform3D[] getMipmapTransforms() {
//        return mipmapTransforms;
//    }
//
//    @Override
//    public int numMipmapLevels() {
//        return mipmapResolutions.length;
//    }
//
//    @Override
//    public VoxelDimensions getVoxelSize(final int timepointId) {
//        return null;
//    }
//
//    /**
//     * Create a {@link CellImg} backed by the cache.
//     */
//    private <T extends NativeType<T>> RandomAccessibleInterval<T> prepareCachedImage(final int timepointId, final int level, final LoadingStrategy loadingStrategy, final T type) {
//        try {
//            final String pathName = getPathName(setupId, level);
//            final DatasetAttributes attributes = getDatasetAttributes(pathName);
//
//            if (logChunkLoading) {
//                System.out.println("Preparing image " + pathName + " of data type " + attributes.getDataType());
//            }
//            long[] dimensions = getDimensions(attributes);
//            final int[] cellDimensions = getBlockSize(attributes);
//            final CellGrid grid = new CellGrid(dimensions, cellDimensions);
//
//            final int priority = numMipmapLevels() - 1 - level;
//            final CacheHints cacheHints = new CacheHints(loadingStrategy, priority, false);
//
//            final SimpleCacheArrayLoader<?> loader = createCacheArrayLoader(n5, pathName, setupToChannel.get(setupId), timepointId, grid);
//            return cache.createImg(grid, timepointId, setupId, level, cacheHints, loader, type);
//        } catch (IOException e) {
//            System.err.printf(
//                    "image data for timepoint %d setup %d level %d could not be found.%n",
//                    timepointId, setupId, level);
//            return Views.interval(
//                    new ConstantRandomAccessible<>(type.createVariable(), 3),
//                    new FinalInterval(1, 1, 1));
//        }
//    }
//    private SimpleCacheArrayLoader<?> createCacheArrayLoader(final N5Reader n5, final String pathName, int channel, int timepointId, CellGrid grid) throws IOException {
//        final DatasetAttributes attributes = n5.getDatasetAttributes(pathName);
//        return new N5OMEZarrCacheArrayLoader<>(n5, pathName, channel, timepointId, attributes, grid, zarrAxes);
//    }
//    private long[] getDimensions(DatasetAttributes attributes) {
//        if (zarrAxes != null) {
//            if ((zarrAxes.equals(ZarrAxes.YX)) || (zarrAxes.is4DWithTimepointsAndChannels())) {
//                return fillDimensions(attributes);
//            }
//        }
//        return Arrays.stream(attributes.getDimensions()).limit(3).toArray();
//    }
//
//
//    private long[] fillDimensions(DatasetAttributes attributes) {
//        long[] tmp = new long[3];
//        tmp[0] = Arrays.stream(attributes.getDimensions()).toArray()[0];
//        tmp[1] = Arrays.stream(attributes.getDimensions()).toArray()[1];
//        tmp[2] = 1;
//        return tmp;
//    }
//
//
//    private int[] fillBlockSize(DatasetAttributes attributes) {
//        int[] tmp = new int[3];
//        tmp[0] = Arrays.stream(attributes.getBlockSize()).toArray()[0];
//        tmp[1] = Arrays.stream(attributes.getBlockSize()).toArray()[1];
//        tmp[2] = 1;
//        return tmp;
//    }
//
//    private int[] getBlockSize(DatasetAttributes attributes) {
//        if ((zarrAxes.equals(ZarrAxes.YX)) || (zarrAxes.is4DWithTimepointsAndChannels())) {
//            return fillBlockSize(attributes);
//        }
//        return Arrays.stream(attributes.getBlockSize()).limit(3).toArray();
//    }
//}