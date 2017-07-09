package main.com.lcc.hahu.controller;

import main.com.lcc.hahu.model.Message;
import main.com.lcc.hahu.model.User;
import main.com.lcc.hahu.service.MessageService;
import main.com.lcc.hahu.service.UserService;
import main.com.lcc.hahu.util.MyConstant;
import main.com.lcc.hahu.util.QiniuyunUtil;
import main.com.lcc.hahu.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by liangchengcheng on 2017/7/10.
 */
@Controller
@RequestMapping("/")
public class IndexController {

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    @RequestMapping(value = {"/","/index"})
    public String index(){
        return "index";
    }

    @RequestMapping("getIndexDetail")
    @ResponseBody
    public Response getIndexDetail(Integer page, HttpServletRequest request){
        Integer userId = userService.getUserIdFromRedis(request);
        Map<String,Object> map = userService.getIndexDetail(userId,page);
        return new Response(0,"",map);
    }

    public String setting(HttpServletRequest request, Model model){
        Integer userId =userService.getUserIdFromRedis(request);
        User user = userService.getProfileInfo(userId);
        model.addAttribute("user",user);
        return "deitProfile";
    }


    @RequestMapping("/editProfile")
    public String editProfile(User user, HttpServletRequest request) {
        Integer userId = userService.getUserIdFromRedis(request);
        user.setUserId(userId);

        userService.updateProfile(user);
        return "redirect:/profile/" + userId;
    }

    /**
     * 更新密码
     */
    @RequestMapping("/updatePassword")
    public String updatePassword(String password,String newpassword,HttpServletRequest request,Model model){
        Integer userId = userService.getUserIdFromRedis(request);
        Map<String,String> map =userService.updatePassword(userId,password,newpassword);
        if (map.get("error") != null){
            model.addAttribute("error",map.get("error"));
            return "forward:/setting";
        }
        return "redirect:/logout";
    }

    @RequestMapping("/updateAvatarUrl")
    public String updateAvatarUrl(MultipartFile paramName, HttpServletRequest request) throws IOException {
        Integer userId = userService.getUserIdFromRedis(request);

        //包含原始文件名的字符串
        String fi = paramName.getOriginalFilename();
        //提取文件拓展名
        String fileNameExtension = fi.substring(fi.indexOf("."),fi.length());
        //生成云端真实文件名
        String remoteFileName = UUID.randomUUID().toString() + fileNameExtension;
        QiniuyunUtil.upload(paramName.getBytes(),remoteFileName);
        //返回图片的url的地址
        String avatarUrl = MyConstant.QINIU_IMAGE_URL + remoteFileName;

        userService.updateAvatarUrl(userId,avatarUrl);
        return "redirect:/profile/" + userId;
    }

    @RequestMapping("/explore")
    public String explore() {
        return "explore";
    }

    @RequestMapping("/message")
    public String message(HttpServletRequest request, Model model) {
        Integer userId = userService.getUserIdFromRedis(request);
        Map<String, List<Message>> map = messageService.listMessage(userId);
        model.addAttribute("map", map);
        return "message";
    }

    @RequestMapping(value = "/uploadImage", method = { RequestMethod.POST }, produces = "text/plain;charset=UTF-8")
    public @ResponseBody String uploadImage(MultipartFile paramName) throws IOException {
        //文件的类型限制
        String[] allowedType = {"image/bmp", "image/gif", "image/jpeg", "image/png" };
        boolean allowed = Arrays.asList(allowedType).contains(paramName.getContentType());
        if (!allowed) {
            return "error|不支持的类型";
        }

        //限制图片的大小
        if (paramName.getSize() > 3 * 1024 * 1024){
            return "error|图片大小不能超过3M";
        }

        //包含原始文件名的字符串
        String fi = paramName.getOriginalFilename();
        // 提取文件拓展名
        String fileNameExtension = fi.substring(fi.indexOf("."), fi.length());
        // 生成云端的真实文件名
        String remoteFileName = UUID.randomUUID().toString() + fileNameExtension;

        QiniuyunUtil.upload(paramName.getBytes(), remoteFileName);
        // 返回图片的URL地址
        return MyConstant.QINIU_IMAGE_URL + remoteFileName;
    }

}
