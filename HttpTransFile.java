package com.npacn.amc.common.utils.FtpTransFile;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Created by wj on 2017/11/13.
 * 暂时只完成基本的文件上传下载功能
 *
 */
public class HttpTransFile {

    private static String DIR = "";

    public HttpTransFile(){
        ResourceBundle rb = PropertyResourceBundle.getBundle("amc");
        this.DIR = rb.getString("realDir");
        File fileRoot = new File(DIR);
        if (!fileRoot.exists()) {
            fileRoot.mkdir();
        }
    }

    /**
     * 初始化参数
     */
    public void init(){

    }

    public String uploadBefore(MultipartFile srcFile, String tagPath, String fileNames) throws IOException {
        DiskFileItem fileItem = (DiskFileItem) ((CommonsMultipartFile) srcFile).getFileItem();
        InputStream fis = srcFile.getInputStream();
        saveAs(fis,tagPath,fileNames);
        //删除文件上传时生成的临时文件
        fileItem.delete();
        return "true";
    }

    /**
     * 使用springMVC自带的另外的一个文件上传对象的tranferTo方式上传文件--比流上传更简单快速
     * @param srcFile
     * @param tagPath
     * @param fileNames
     * @return
     * @throws IOException
     */
    public String transFor(CommonsMultipartFile srcFile, String tagPath, String fileNames) throws IOException {

        try {
            File fileRoot = new File(DIR + tagPath);
            if (!fileRoot.exists()) {
                fileRoot.mkdir();
            }
            tagPath = DIR + tagPath;
            File targetFile = new File(tagPath +"/"+ fileNames);
            srcFile.transferTo(targetFile);
            return "true";
        }catch (Exception e){
            e.printStackTrace();
            return "false";
        }
    }
    /**
     * 保存文件到指定路径
     * @param is
     * @param tagPath
     * @param fileNames
     */
    public static void saveAs(InputStream is, String tagPath, String fileNames) throws IOException {
        try {
            //先判断当前保存文件的目录是否存在，不存在则新建目录
            File fileRoot = new File(DIR + tagPath);
            if (!fileRoot.exists()) {
                fileRoot.mkdir();
            }
            tagPath = DIR + tagPath;
            FileOutputStream fos = new FileOutputStream(tagPath+"/"+fileNames);
            byte buffer[] = new byte[1024];
            int len = 0;
            while ((len = is.read(buffer)) > 0) {
                fos.write(buffer);
            }
            is.close();
            fos.close();
        }catch (Exception e){
            e.printStackTrace();
        }


    }
}
