package club.misakinetwork.aliyunoss;

import club.misakinetwork.aliyunoss.tools.AliyunOSS_Util;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class AliyunOssApplication {

    public static void main(String[] args) {
        SpringApplication.run(AliyunOssApplication.class, args);
    }

    @Test
    public void test(){
        AliyunOSS_Util.uploadImages("misakinetwork","images",new File("C:\\Users\\Falln\\Downloads\\photo.png"));
    }

}
