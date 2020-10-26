
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayDeque;

final class CloudDetectionUtils {
    private static double cloudRatio;
    private static double imgAvalibility;

    private final static int LOW_GRAY_THRE = 140;

    private final static int AREA_THRE = 20;

    private final static int HIGH_GRAY_THRE = 180;

    private int []bitImg;

    private int[] labelMap;
    
    int width;
    int height;

    private ArrayDeque queue;
    
	public void report(String r) {
		System.out.println(r);
	}

    static double getCloudRatio() {
        return CloudDetectionUtils.cloudRatio;
    }

    static double getImgAvalibility() {
        return CloudDetectionUtils.imgAvalibility;
    }
    
    CloudDetectionUtils(int width, int height) {
    	this.width = width;
    	this.height = height;
    }

	public int[] filetoByteArray(String filename) throws IOException {
   	 
        FileChannel fc = null;
        try {
            fc = new RandomAccessFile(filename, "r").getChannel();
            MappedByteBuffer byteBuffer = fc.map(MapMode.READ_ONLY, 0,
                    fc.size()).load();
            System.out.println(byteBuffer.isLoaded());
            byte[] result = new byte[(int) fc.size()];
            int [] ret = new int[(int) fc.size()];
            if (byteBuffer.remaining() > 0) {
                // System.out.println("remain");
                byteBuffer.get(result, 0, byteBuffer.remaining());
            }
            int i = 0;
            for(byte b : result) {
            	ret[i] = result[i];
            	i++;
            }
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                fc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
	
    void thres(String fileName) throws IOException {
        cloudRatio = 0.0;
        imgAvalibility = 0.0;

        int width = this.width;
        int height = this.height;

        int[] curImage = filetoByteArray(fileName);

        bitImg = new int[width * height];
        
        for (int i = 0; i < height; i++)
        {
            for (int j = 0; j < width; j++)
            {
                if (curImage[i * width + j]> LOW_GRAY_THRE)
                    bitImg[i * width + j] = 255;
            }
        }

        report("height:" + String.valueOf(height));
        report("width:" + String.valueOf(width));
        
        labelMap = new int[height * width];
        queue = new ArrayDeque<Integer>();

            int labelIndex = connectedComponentLabeling(height, width);

            int[] area = new int[labelIndex + 1];

            for (int i = 0; i < height; i++)
            {
                for (int j = 0; j < width; j++)
                {
                    if (labelMap[i * width + j] != 0)
                        area[labelMap[i * width + j]] = area[labelMap[i * width + j]] + 1;
                }
            }

            for (int i = 0; i < height; i++)
            {
                for (int j = 0; j < width; j++)
                {
                    if (labelMap[i * width + j] != 0 && area[labelMap[i * width + j]] <AREA_THRE)
                    {
                        labelMap[i * width + j] = 0;
                        bitImg[i * width + j] = 0;
                    }
                }
            }

            double[] grayMean = new double[labelIndex + 1];

            for (int i = 0; i < height; i++)
            {
                for (int j = 0; j < width; j++)
                {
                    if (labelMap[i * width + j] != 0)
                    {
                        grayMean[labelMap[i * width + j]]
                                = grayMean[labelMap[i * width + j]]
                                + (double)(curImage[i * width + j])
                                / (double)(area[labelMap[i * width + j]]);
                    }
                }
            }

            double areaGraySum = 0.0;
            double areaSum = 0.0;
            for (int k = 1; k <= labelIndex; k++)
            {
                if (area[k] != 0)
                {
                    areaGraySum = areaGraySum + (double)(area[k]) * grayMean[k];
                    areaSum = areaSum + area[k];
                }
            }

            cloudRatio = areaSum / (height*width);

            imgAvalibility = (height*width / areaGraySum) * HIGH_GRAY_THRE;
    }

    private final static int[][] NeighborDirection
            = { { 0, 1 }, { 1, 1 }, { 1, 0 }, { 1, -1 },
            { 0, -1 }, { -1, -1 }, { -1, 0 }, { -1, 1 } };


    private void searchNeighbor(int height, int width, int labelIndex, int pixelIndex){
        int searchIndex;
        labelMap[pixelIndex] = labelIndex;
        int length = width * height;

        for (int i = 0; i < 8; i++){
            searchIndex = pixelIndex + NeighborDirection[i][0] * width + NeighborDirection[i][1];
            if (searchIndex > 0 && searchIndex < length &&
                    bitImg[searchIndex] == 255
                    && labelMap[searchIndex] == 0){
                labelMap[searchIndex] = labelIndex;

                queue.add(searchIndex);
            }
        }
    }


    private int connectedComponentLabeling(int height, int width) {
        int index;
        int popIndex;
        int labelIndex  = 0;
        Object o;

        for (int cy = 1; cy < height - 1; cy++){
            for (int cx = 1; cx < width - 1; cx++){

                index = cy * width + cx - 1;

                if (bitImg[index] == 255 && labelMap[index] == 0){

                    labelIndex++;
                    searchNeighbor(height, width, labelIndex, index);

                    o = queue.poll();
                    if(o != null) {
                        popIndex = (int) o;
                    }
                    else {
                        popIndex = -1;
                    }

                    while (popIndex > -1){
                        searchNeighbor(height, width, labelIndex, popIndex);
                        o = queue.poll();
                        if(o != null) {
                            popIndex = (int) o;
                        }
                        else {
                            popIndex = -1;
                        }
                    }
                }
            }
        }
        return labelIndex;
    }
}
