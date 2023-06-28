package com.metoo.nspm.core.utils.file;

import com.metoo.nspm.entity.nspm.Accessory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;

@Component
public class UploadFileUtil {

    public boolean uploadFile(@RequestParam(required = false) MultipartFile file, String fileName, String path){
        try {
            File fil = new File(path + File.separator + fileName + ".png");
            if (!fil.getParentFile().exists()) {
                fil.getParentFile().mkdirs();
            }
            file.transferTo(fil);
            return true;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteFile(String fileName, String path){
        File file = new File(path + File.separator + fileName + ".png");
//        File[] listFiles = file.listFiles();
//        if(listFiles != null)
//        {
//            for(File f: listFiles)
//            {
//                if(f.isDirectory())
//                {
//                    delete(f);
//                }
//                else
//                {
//                    f.delete();
//                }
//            }
//        }
        if(file != null){
            return file.delete();
        }
        return true;
    }

}
