package club.misakinetwork.aliyunoss.tools;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Objects;
import java.util.Properties;


/**
 * 关于endpoint
 * Endpoint为Bucket的外链，我写了一个常用的endpoint类可以参考里面的外链
 * 里面有：
 * 香港、上海、深圳、成都、杭州、美国硅谷、日本东京
 *
 */
@Component
public class AliyunOSS_Util{

    /**
     *  关于ossClient,
     *  在以前是使用
     *  OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySeret);
     *  这种方式来初始化ossClient客户端，已经被弃用了
     *  现在使用
     *  OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
     *  初始化
     */
    private static OSS ossClient;


    /**
     * 关于初始化配置参数，我提供了2种方式
     *  1.springboot配置文件方式
     *  使用的@Value进行赋值 如 @Value("${a.abc}")
     *  注意：static属性@Value是注解不上去的
     *  2.aliyunOSS.properties 配置文件方式 具体在static{}代码块里面
     */

    private static String accessKeyId;

    private static String accessKeySecret;

    private static String endpoint;

    private static Properties properties;



    private static final Logger logger = LoggerFactory.getLogger(AliyunOSS_Util.class);


    //配置文件初始化
    static{

        try {
            //获取accessKeyId and accessKeySecret
            properties = new Properties();
            properties.load(new InputStreamReader(Objects.requireNonNull(AliyunOSS_Util.class
                    .getClassLoader().getResourceAsStream("aliyunOSS.properties"))));

        } catch (IOException e) {
            e.printStackTrace();
        }

        StringBuffer json = new StringBuffer("{");

        //初始化参数
        accessKeyId = properties.getProperty("accessKeyId");
        json.append("accessKeyId:"+accessKeyId);

        accessKeySecret = properties.getProperty("accessKeySecret");
        json.append(",accessKeySecret:"+accessKeySecret);

        endpoint = properties.getProperty("endpoint");
        json.append(",endpoint:"+endpoint+"}");

        logger.info(String.valueOf(json));
    }



    /**
     * 获取OssClient
     * @return
     */
    private static OSS getOssClient(){
        return  ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }


    /**
     * 创建存储空间
     * @param bucketName 存储空间名称，规范请查看官方文档
     * @return
     */
    public static String createBucketName(String bucketName){
        //存储空间
        final String bucketNames=bucketName;
        if(!ossClient.doesBucketExist(bucketName)){
            //创建存储空间
            Bucket bucket=getOssClient().createBucket(bucketName);
            logger.info("创建存储空间成功");
            return bucket.getName();
        }
        return bucketNames;
    }

    /**
     * 删除存储空间
     * @param bucketName  存储空间
     */
    public static void deleteBucket(String bucketName){
        getOssClient().deleteBucket(bucketName);
        logger.info("删除" + bucketName + "Bucket成功");
    }

    /**
     * 创建文件夹
     * @param bucketName 存储空间
     * @param folder  文件夹名
     * @return
     */
    public static String createFolder(String bucketName,String folder){
        //文件夹名
        final String FolderName =folder+"/";
        //判断文件夹是否存在，不存在则创建
        if(!getOssClient().doesObjectExist(bucketName, FolderName)){
            //创建文件夹
            getOssClient().putObject(bucketName, FolderName, new ByteArrayInputStream(new byte[0]));
            logger.info("创建文件夹成功");
            //得到文件夹名
            OSSObject object = ossClient.getObject(bucketName, FolderName);
            String fileDir=object.getKey();
            ossClient.shutdown();
            return fileDir;
        }
        logger.info("文件夹已存在");
        ossClient.shutdown();
        return FolderName;
    }

    public static void upload(String bucketName,String folder,File file) throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(file);
        if(!getOssClient().doesObjectExist(bucketName, folder)){
            //创建文件夹
            getOssClient().putObject(bucketName, folder, new ByteArrayInputStream(new byte[0]));
            logger.info("创建文件夹成功");
            //得到文件夹名
            OSSObject object = ossClient.getObject(bucketName, folder);
            String fileDir=object.getKey();
        }
        getOssClient().putObject(bucketName, folder+"/"+file.getName(),inputStream);
        logger.info("上传成功");
        ossClient.shutdown();
    }

    /**
     * 根据key删除OSS服务器上的文件
     * 注意：阿里云OSS没有文件夹的概念，
     * 你只要删除了某个文件夹下的所有文件，这个文件夹就会消失，根目录下的除外
     * @param bucketName 存储空间
     * @param folder 文件夹 如：img/jpg
     * @param fileName 文件名 如 abc.jpg
     */
    public static void deleteFile( String bucketName, String folder, String fileName) {
        getOssClient().deleteObject(bucketName, folder +"/"+ fileName);
        logger.info("删除" + bucketName + "下的文件" + folder + fileName + "成功");
        ossClient.shutdown();
    }


    /**
     * 上传图片至OSS
     * @param file 上传文件（D:\\image\\adb.jpg）
     * @param bucketName 存储空间
     * @param folder 文件夹名
     * @return String 返回的唯一MD5数字签名
     */
    public static boolean uploadImages( String bucketName, String folder,File file) {
        String resultStr = null;
        try {
            // 以输入流的形式上传文件
            InputStream is = new FileInputStream(file);

            String lastSuffix = getLastSuffix(file.getName());

            if(!(lastSuffix.equals("jpg") || lastSuffix.equals("png") || lastSuffix.equals("gif"))){
                logger.info(lastSuffix+"不支持该文件类型（jpg、png、gif）");
                return false;
            }
            folder += "/"+lastSuffix+"/";
            // 上传文件 (上传文件流的形式)
            PutObjectResult putResult = getOssClient().putObject(bucketName,
                    folder+file.getName(),is);
            // 解析结果
            logger.info(putResult.getETag());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("上传阿里云OSS服务器异常." + e.getMessage(), e);
        }
        ossClient.shutdown();
        return true;
    }

    /**
     * 获取后缀名
     * @param fileName 文件名
     * @return 文件的后缀名
     */
    public static String getLastSuffix(String fileName) {
        // 文件的后缀名
        String fileExtension = fileName.substring(fileName.lastIndexOf("."));
        if (".gif".equalsIgnoreCase(fileExtension)) {
            return "gif";
        }
        if (".jpg".equalsIgnoreCase(fileExtension)) {
            return "jpe";
        }
        if (".png".equalsIgnoreCase(fileExtension)) {
            return "png";
        }
        // 默认返回类型
        return "";
    }


}

