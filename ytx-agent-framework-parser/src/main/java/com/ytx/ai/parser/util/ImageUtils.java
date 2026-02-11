package com.ytx.ai.parser.util;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.parser.constant.ParseConstants;
import com.ytx.ai.parser.vo.Coordinate;
import com.ytx.ai.parser.vo.ImageVO;
import com.ytx.ai.parser.vo.ScaleOption;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.ytx.ai.parser.constant.ParseConstants.DEFAULT_IMAGE_TYPE;
import static com.ytx.ai.parser.constant.ParseConstants.IMG_SCALE_MIN_WIDTH;

@Slf4j
public class ImageUtils {

    static {
        NativeLoader.loadNativeLibrary();
    }

    public static final int MB = 1024 * 1024; // 1 MB

    public static String toBase64WithScheme(byte[] bytes, String ext) {
        return ImageDataURISchemeMapper.toBase64WithScheme(bytes);
    }

    public static String toBase64(byte[] bytes) {
        return bytesEncode2Base64(bytes);
    }

    private static String bytesEncode2Base64(byte[] bytes) {
        return new String(Base64.getEncoder().encode(bytes), StandardCharsets.UTF_8);
    }

    /**
     * 检查是否是 base64 data URI 格式
     * 格式：data:image/png;base64,xxx 或 data:image/jpeg;base64,xxx 等
     */
    public static boolean isBase64DataURI(String url) {
        if (ObjectUtil.isEmpty(url)) {
            return false;
        }
        // 检查是否以 data:image/ 开头，并且包含 base64,
        return url.startsWith("data:image/") && url.contains("base64,");
    }
    public static ImageVO base64WithSchemeToImage(String imageBase64WithScheme){

        if(ObjectUtil.isEmpty(imageBase64WithScheme)){
            return null;
        }
        if(imageBase64WithScheme.indexOf("base64,")<1){
            return null;
        }
        int index=imageBase64WithScheme.indexOf("base64,")+"base64,".length();
        String imageBase64=imageBase64WithScheme.substring(index);
        ImageVO image=new ImageVO();
        image.setBytes(Base64.getDecoder().decode(imageBase64));
        image.setExt(ImageDataURISchemeMapper.getExtensionFromImageBase64(imageBase64WithScheme,DEFAULT_IMAGE_TYPE));
        return image;
    }

    /**
     * 将 BufferedImage 转换为字节数组
     *
     * @param image      BufferedImage 对象
     * @param formatName 图像格式名称（如 "png", "jpg"）
     * @return 字节数组
     */
    public static byte[] toBytes(BufferedImage image, String formatName) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // 将 BufferedImage 写入字节输出流
            ImageIO.write(image, formatName, baos);
            // 转换为字节数组并返回
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("转换 BufferedImage 为字节数组时出错: {}", e.getMessage());
            throw new RuntimeException("Error converting BufferedImage to byte array", e);
        }
    }


    public static ImageVO mergeImage(List<ImageVO> images, String extName) {
        if (ObjectUtil.isEmpty(images)) {
            return null;
        }
        if (images.size() == 1) {
            return images.getFirst();
        }
        ImageVO mergedImage = new ImageVO();
        Mat stitchedImage = new Mat();
        List<Mat> tempMatList=null;
        List<Mat> matsToMerge = new ArrayList<>();
        try{
            int maxWidth = 0;
            tempMatList = imagesToMatList(images);
            for (Mat mat : tempMatList) {
                if (mat.width() > maxWidth) {
                    maxWidth = mat.width();
                }
            }
            padImage(tempMatList, matsToMerge, maxWidth);
            if (matsToMerge.isEmpty()) {
                log.error("No images could be decoded, returning null.");
                return null;
            }
            // 核心：调用vconcat进行纵向拼接
            Core.vconcat(matsToMerge, stitchedImage);
            // 4. 拼接后检查结果是否为空
            if (stitchedImage.empty()) {
                log.error("Concatenated image (stitchedImage) is empty. Aborting encoding.");
                return null;
            }
            if(needScale(extName,stitchedImage.width(), stitchedImage.height())){
                int maxDimension=getMaxDimension(extName);
                double scale = (double) maxDimension/ stitchedImage.height();
                int newWidth = (int) (stitchedImage.width() * scale);
                Mat resizedImage =null;
                try {
                    resizedImage=resizeImage(stitchedImage, newWidth, maxDimension);
                    // 使用缩放后的图片进行编码
                    byte[] imageBytes = imageMatToBytes(resizedImage, extName);
                    mergedImage.setBytes(imageBytes);
                } catch (Exception e) {
                    throw new Exception(e);
                }finally {
                    if(resizedImage!=null){
                        resizedImage.release();
                    }
                }
            }else{
                log.info("stitchedImage image dimensions (WxH): {}x{}", stitchedImage.width(), stitchedImage.height()); // 打印尺寸
                System.out.println("stitchedImage empty?"+stitchedImage.empty());
                byte[] imageBytes= imageMatToBytes(stitchedImage,extName);
                mergedImage.setBytes(imageBytes);
            }
            mergedImage.setExt(extName);
            return mergedImage;
        } catch (Exception e) {
            log.error("merge image error: {}", e.getMessage());
        }finally {
            stitchedImage.release();
            for(Mat mat :matsToMerge){
                mat.release();
            }
            if(tempMatList!=null){
                for(Mat mat :tempMatList){
                    mat.release();
                }
            }
        }
        return null;
    }


    private static Mat resizeImage(Mat mat,int width,int height){
        Mat resizedImage =null;
        try{
            resizedImage = new Mat();
            Size newSize = new Size(width, height);
            Imgproc.resize(mat, resizedImage, newSize, 0.0, 0.0, Imgproc.INTER_AREA);
            log.info("resizedImage image dimensions (WxH): {}x{}", resizedImage.width(), resizedImage.height()); // 打印尺寸
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        return resizedImage;

    }

    private static byte[] imageMatToBytes(Mat mat,String extName){

        byte[] imageBytes=null;
        // 编码到内存
        MatOfByte mob =new MatOfByte();
        try{
            Imgcodecs.imencode("." + extName, mat, mob);
            imageBytes=mob.toArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            mob.release();
        }
        return imageBytes;
    }

    private static List<Mat> imagesToMatList(List<ImageVO> images){
        List<Mat> matList=new ArrayList<>();
        // 将字节数组解码为Mat对象
        for (ImageVO image : images) {
            Mat mat =bytesToImageMat(image.getBytes());
            if(mat!=null && !mat.empty()){
                matList.add(mat);
            }else{
                log.error("image转mat后为空");
            }
        }
        return matList;
    }

    private static Mat bytesToImageMat(byte[] bytes){
        Mat mat =null;
        MatOfByte matOfByte = null;
        try {
            matOfByte=new MatOfByte(bytes);
            mat = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR);
        }finally {
            if(matOfByte!=null){
                matOfByte.release();
            }
        }
        return mat;
    }

    private static void padImage(List<Mat> original, List<Mat> resized, int maxWidth){
        // 定义白色
        Scalar white = new Scalar(255.0, 255.0, 255.0, 0.0);
        // 3. 遍历所有图片，对宽度不足的进行白色填充
        for (Mat originalImage : original) {
            if (originalImage.width() < maxWidth) {
                // 计算需要填充的宽度
                int paddingWidth = maxWidth - originalImage.width();
                int paddingLeft = paddingWidth / 2;
                int paddingRight = paddingWidth - paddingLeft; // 这样可以处理奇数宽度差
                Mat paddedImage = new Mat();
                // 使用 copyMakeBorder 函数进行填充
                Core.copyMakeBorder(originalImage, paddedImage, 0, 0, paddingLeft, paddingRight, Core.BORDER_CONSTANT, white);
                resized.add(paddedImage);
                originalImage.release();
            } else {
                // 如果图片本身就是最大宽度，直接添加到新列表
                resized.add(originalImage);
            }
        }
    }

    private static boolean needScale(String extName,int width,int height){
        int maxDimension=getMaxDimension(extName);
        if(maxDimension<=0){
            return false;
        }
        return width>maxDimension || height>maxDimension;
    }

    private static int getMaxDimension(String extName){

        return switch (extName) {
            case "jpeg" -> ParseConstants.JPEG_MAX_DIMENSION;
            case "webp" -> ParseConstants.WEBP_MAX_DIMENSION;
            default -> -1;
        };
    }
    /**
     * 尝试只读取图片的宽高（不解码像素数据），若失败返回 null
     */
    private static int[] readImageDimension(byte[] bytes) {
        int[] wh=null;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ImageInputStream iis = ImageIO.createImageInputStream(bais)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new RuntimeException("无法读取图片头信息");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                int w = reader.getWidth(0);
                int h = reader.getHeight(0);
                wh= new int[]{w, h};
                return wh;
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            log.error("无法读取图片头信息");
        }

        BufferedImage bi=null;
        // 回退：如果不能读取尺寸，则完整解码（尽量少发生）
        try (InputStream is = IoUtil.toStream(bytes)) {
            bi = ImageIO.read(is);
            if (bi != null) {
                wh = new int[]{bi.getWidth(), bi.getHeight()};
            }
        } catch (IOException e) {
            log.error("无法读取图片尺寸：{}",e.getMessage());
        }finally {
            if (bi != null) {
                bi.flush();
            }
        }
        return wh;
    }

    private static int findMaxWidth(List<BufferedImage> bufferedImages) {
        if (ObjectUtil.isEmpty(bufferedImages)) {
            return -1;
        }
        return bufferedImages.stream()
                .map(BufferedImage::getWidth)
                .max(Integer::compare)
                .orElse(-1); // 如果没有找到最大值，返回-1
    }



    /**
     * 裁剪图片的顶部和底部,保留中间的部分
     *
     * @param imageBytes 图片的字节数组
     * @param cropTop    顶部需要裁剪的像素高度
     * @param cropBottom 底部需要裁剪的像素高度
     * @return 裁剪后的图片字节数组
     * @throws IOException 如果发生IO异常
     */
    public static byte[] cropImage(byte[] imageBytes,String ext, int cropTop, int cropBottom) throws IOException {
        // 将字节数组转换为 BufferedImage
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
        BufferedImage originalImage = ImageIO.read(inputStream);

        double rate=originalImage.getHeight()*1.0D/1123;
        int actualCropTop=(int) Math.round(cropTop * rate);
        int actualCropBottom=(int)Math.round(cropBottom * rate);

        // 计算裁剪后的高度
        int newHeight = originalImage.getHeight() - actualCropTop - actualCropBottom;
        if (newHeight <= 0) {
            throw new IllegalArgumentException("裁剪后的高度必须大于零！");
        }

        // 裁剪图片
        BufferedImage croppedImage = originalImage.getSubimage(
                0, // x起始位置
                actualCropTop, // y起始位置
                originalImage.getWidth(), // 剪裁后的宽度
                newHeight // 剪裁后的高度
        );

        // 将裁剪后的 BufferedImage 转换为字节数组
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(croppedImage, ext, outputStream);
        return outputStream.toByteArray();
    }

    /**
     * 根据坐标，从图片中裁减掉坐标内的内容，保留图片其他的部分
     * @param imageBytes 图片
     * @param cropCoordinates 裁剪去掉的坐标
     * @return
     * @throws IOException
     */
    public static byte[] cropImage(byte[] imageBytes,String ext, List<Coordinate> cropCoordinates) throws IOException {
        // 将字节数组转换为 BufferedImage
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
        BufferedImage originalImage = ImageIO.read(inputStream);
        int width = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        double rate=originalHeight*1.0D/3508;

        //对坐标排序，从低到高
        cropCoordinates = cropCoordinates.stream().map(item -> {
                    item.setFromY((int) Math.round(item.getFromY() * rate));
                    item.setToY((int) Math.round(item.getToY() * rate));
                    return item;
                })
                .sorted(Comparator.comparingInt(Coordinate::getFromY)).toList();
        // 计算裁剪后新图片的高度
        int croppedHeight = originalHeight;
        for (Coordinate coordinate : cropCoordinates) {
            croppedHeight -= (coordinate.getToY() - coordinate.getFromY());
        }
        // 创建新图片
        BufferedImage croppedImage = new BufferedImage(width, croppedHeight, originalImage.getType());
        // 将需要保留的部分绘制到新图片
        int currentY = 0;
        int currentDrawY=0;
        for (Coordinate coordinate : cropCoordinates) {
            int fromY = coordinate.getFromY();
            int toY = coordinate.getToY();
            int keepAreaY=fromY-currentY;
            if(keepAreaY>0){
                // 复制上方保留的区域
                BufferedImage topPart = originalImage.getSubimage(0, currentY, width, keepAreaY);
                croppedImage.getGraphics().drawImage(topPart, 0, currentDrawY, null);
            }
            // 更新当前 Y 坐标
            currentY = toY;
            currentDrawY+=keepAreaY;
        }
        // 复制最后一段
        if (currentY < originalHeight) {
            BufferedImage bottomPart = originalImage.getSubimage(0, currentY, width, originalHeight - currentY);
            croppedImage.getGraphics().drawImage(bottomPart, 0, currentDrawY, null);
        }
        // 将裁剪后的 BufferedImage 转换为字节数组
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(croppedImage, ext, outputStream);
        return outputStream.toByteArray();
    }

    /**
     * opencv实现版本
     * @param imageVO
     * @param maxMB
     */
    public static void resizeImage(ImageVO imageVO, double maxMB) {
        if (imageVO == null || imageVO.getBytes() == null || maxMB <= 0) {
            return;
        }
        long thresholdBytes = Math.round(maxMB * 1024 * 1024);
        byte[] srcBytes = imageVO.getBytes();
        // 如果已经小于阈值，直接返回
        if (srcBytes.length <= thresholdBytes) {
            return;
        }
        // 读取图片尺寸（优先轻量读取）
        int[] wh = readImageDimension(srcBytes);
        int width = wh != null ? wh[0] : -1;
        int height = wh != null ? wh[1] : -1;
        log.debug("src bytes length:{}",srcBytes.length);
        log.debug("src width:{} height:{}",width,height);
        if (width <= 0 || height <= 0) {
            throw new RuntimeException("无法读取图片"+imageVO.getIndex()+"."+imageVO.getExt()+"尺寸信息");
        }
        long currentSize = srcBytes.length;

        Mat sourceImage =null;
        try{
            sourceImage=bytesToImageMat(imageVO.getBytes());
            // 计算缩放比例（不放大）
            double scale=calculateScaleFactor(width,height,currentSize, thresholdBytes,imageVO.getExt());
            int targetW = Math.max(IMG_SCALE_MIN_WIDTH, (int) Math.round(width * scale));
            scale=targetW*1.0/width;
            if (sourceImage.empty()) {
                System.out.println("加载图片失败！");
                return;
            }
            double newWidth=sourceImage.width() * scale;
            double newHeight=sourceImage.height() * scale;
            Mat resizedImage =null;
            try{
                resizedImage =resizeImage(sourceImage,(int)newWidth,(int)newHeight);
                // 使用缩放后的图片进行编码
                byte[] imageBytes = imageMatToBytes(resizedImage, imageVO.getExt());
                imageVO.setBytes(imageBytes);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }finally {
                if(resizedImage!=null){
                    resizedImage.release();
                }
            }
        } catch (Exception e) {
            log.error("resizeImage error: {}", e.getMessage());
        }
    }

    private static double calculateScaleFactor(int width,int height,long currentSize,long targetSize,String extName){
        double factor = switch (extName) {
            case "webp" -> 1;
            case "jpeg" -> Math.min(Math.sqrt((double) Math.max(width, height) / Math.min(width, height)),1.2);
            case "png" -> 0.70;
            default -> 1;
        };

        double scale = Math.sqrt(targetSize / (double) currentSize)*factor;
        scale = Math.min(scale, 1.0);
        return scale;
    }


// ------------------ 辅助方法 ------------------
    /** 按质量阶梯压缩直到满足大小限制 */
    private static byte[] compressImageQuality(BufferedImage img, String ext, long thresholdBytes) throws IOException {
        if (ObjectUtil.isEmpty(ext) || !isLossyFormat(ext)) {
            return toBytes(img,ext);
        }
        float[] qualities = {0.95f, 0.9f,0.85f, 0.8f, 0.75f};
        for (float q : qualities) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                Thumbnails.of(img)
                        .size(img.getWidth(), img.getHeight())
                        .outputFormat(ext)
                        .outputQuality(q)
                        .toOutputStream(baos);
                byte[] out = baos.toByteArray();
                if (out.length <= thresholdBytes) {
                    return out;
                }
            }
        }
        // 兜底：返回最后一次压缩结果（即使超限制）
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Thumbnails.of(img)
                    .size(img.getWidth(), img.getHeight())
                    .outputFormat(ext)
                    .outputQuality(0.7f)
                    .toOutputStream(baos);
            return baos.toByteArray();
        }
    }

    private static boolean isLossyFormat(String format) {
        if (format == null) return false;
        String f = format.toLowerCase();
        return f.equals("jpg") || f.equals("jpeg") || f.equals("webp");
    }

    /**
     * 使用 ImageReader 的 source subsampling 来尽量低内存地读取缩小后的图片。
     * 若 subsampling 后尺寸仍不精确，使用 Thumbnailator 在小图上做精确缩放（内存小）。
     */
    private static BufferedImage readScaledBufferedImage(byte[] bytes, int targetWidth, int targetHeight) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ImageInputStream iis = ImageIO.createImageInputStream(bais)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                int origW = reader.getWidth(0);
                int origH = reader.getHeight(0);

                // 若目标尺寸大于等于原图尺寸，则直接完整读取（并由调用方决定是否缩放）
                if (targetWidth >= origW && targetHeight >= origH) {
                    return reader.read(0);
                }

                ImageReadParam param = reader.getDefaultReadParam();
                // 计算子采样因子，保证不会比目标尺寸小太多
                int xSub = Math.max(1, origW / targetWidth);
                int ySub = Math.max(1, origH / targetHeight);
                param.setSourceSubsampling(xSub, ySub, 0, 0);

                BufferedImage subsampled = reader.read(0, param);
                // 如果尺寸与目标仍不精确，在小图上再精确缩放（占用内存小）
                if (subsampled.getWidth() != targetWidth || subsampled.getHeight() != targetHeight) {
                    BufferedImage refined = Thumbnails.of(subsampled)
                            .size(targetWidth, targetHeight)
                            .asBufferedImage();
                    subsampled.flush();
                    return refined;
                }
                return subsampled;
            } finally {
                reader.dispose();
            }
        }
    }



    public static void batchScaleImage(List<ImageVO> images, ScaleOption scaleOption){
        if (ObjectUtil.isEmpty(images) || ObjectUtil.isEmpty(scaleOption) ) {
            return;
        }
        for (ImageVO imageVO : images) {
            scaleImage(imageVO, scaleOption);
        }
    }

    public static void scaleImage(ImageVO imageVO, ScaleOption scaleOption) {
        if (ObjectUtil.isEmpty(imageVO) || ObjectUtil.isEmpty(imageVO.getBytes())
                || ObjectUtil.isEmpty(scaleOption)) {
            return;
        }
        Double scale= scaleOption.getScale();
        BufferedImage originalImage = null;
        BufferedImage resizedImage =null;
        try(InputStream inputStream = IoUtil.toStream(imageVO.getBytes())) {
            if (inputStream == null) {
                return;
            }
            originalImage = ImageIO.read(inputStream);
            if(scale==null || scale <= 0 ){
                scale = calculateScale(originalImage, scaleOption);
            }
            if(scale<=0 || scale==1){
                return;
            }

            resizedImage = Thumbnails.of(originalImage)
                    .scale(scale) // 按比例缩放
                    .asBufferedImage();
            imageVO.setBytes(toBytes(resizedImage, imageVO.getExt()));
        } catch (Exception e) {
            log.error("Error scale image: {}", e.getMessage());
            throw new RuntimeException("Error scale image", e);
        }finally {
            if (originalImage != null) {
                originalImage.flush();
            }
            if (resizedImage != null) {
                resizedImage.flush();
            }
        }
    }


    private static double calculateScale(BufferedImage image, ScaleOption scaleOption) {
        if (ObjectUtil.isEmpty(image) || ObjectUtil.isEmpty(scaleOption) || ObjectUtil.isEmpty(scaleOption.getMaxDimension())) {
            return -1;
        }
        Integer maxDimension = scaleOption.getMaxDimension();
        if(maxDimension==null){
            return -1;
        }
        int imageWidth = image.getWidth();
//        int imageHeight = image.getHeight();
//        int imgMaxDimension = Math.max(imageWidth, imageHeight);
        if(imageWidth <= maxDimension){
            return 1.0; // 如果图片本身就小于等于最大尺寸，则不需要缩放
        }
        return (double) maxDimension / imageWidth;
    }


    public static void convertTo(ImageVO image, String type){
        BufferedImage src =null;
        try(InputStream inputStream = IoUtil.toStream(image.getBytes());
            ByteArrayOutputStream baos = new ByteArrayOutputStream()){
            src = ImageIO.read(inputStream);
            if (src == null) {
                throw new RuntimeException("无法读取图片内容");
            }
            // 使用 Thumbnailator 进行格式转换和压缩
            Thumbnails.of(src)
                    .size(src.getWidth(), src.getHeight())   // 保持原始尺寸
                    .outputFormat(type)
                    .outputQuality(clampQuality(1))
                    .toOutputStream(baos);
            image.setBytes(baos.toByteArray());
            image.setExt(type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            if(src!=null){
                src.flush();
            }
        }
    }
    private static float clampQuality(float q) {
        if (Float.isNaN(q) || q <= 0f) return 0.75f;
        if (q > 1f) return 1f;
        return q;
    }


    public static void printImageInfo(ImageVO image){
        BufferedImage bufferedImage =null;
        try(InputStream inputStream = IoUtil.toStream(image.getBytes())){
            bufferedImage = ImageIO.read(inputStream);
            log.debug("image width:{}",bufferedImage.getWidth());
            log.debug("image height:{}",bufferedImage.getHeight());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            if(bufferedImage!=null){
                bufferedImage.flush();
            }
        }
    }

    public static byte[] toGray(byte[] bytes){
        return toGray(bytes,DEFAULT_IMAGE_TYPE);
    }
    public static byte[] toGray(byte[] bytes,String outputExt){
        try(ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream()){
            Thumbnails.of(inputStream)
                    .outputFormat(outputExt)
                    .scale(1)
                    .imageType(BufferedImage.TYPE_BYTE_GRAY) // 设置为灰度图片
                    .toOutputStream(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

