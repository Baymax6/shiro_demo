package com.alex.admin.controller;

import com.alex.admin.entity.UUser;
import com.alex.admin.service.UserQueryService;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.subject.Subject;
import org.apache.tomcat.jni.Global;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;
import java.util.Objects;

@Controller
public class SecurityController
{
    private static final Logger logger = LoggerFactory.getLogger(SecurityController.class);

    @Autowired
    private UserQueryService userQueryService;

    @RequiresRoles("系统管理员")
    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public String index(Model model)
    {
        String userName = (String) SecurityUtils.getSubject().getPrincipal();
        model.addAttribute("username", userName);
        return "index";
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public String defaultIndex(Model model)
    {
        String userName = (String) SecurityUtils.getSubject().getPrincipal();
        model.addAttribute("username", userName);
        return "index";
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String loginForm()
    {
        return "login";
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    public String login(@Valid UUser user, BindingResult bindingResult, RedirectAttributes redirectAttributes)
    {
        if (bindingResult.hasErrors())
        {
            return "login";
        }

        //获取当前Subject
        Subject currentUser = SecurityUtils.getSubject();

        String userName = user.getEmail();

        UUser resultUser = userQueryService.getUserByName(userName);
        if (Objects.isNull(resultUser))
        {
            return "用户名或密码不正确，请稍后再试!";
        }

        UsernamePasswordToken token = new UsernamePasswordToken(resultUser.getEmail(), resultUser.getPswd());
        String result = StringUtils.EMPTY;
        try
        {
            //在调用了login方法后,SecurityManager会收到AuthenticationToken,并将其发送给已配置的Realm执行必须的认证检查
            //每个Realm都能在必要时对提交的AuthenticationTokens作出反应
            //所以这一步在调用login(token)方法时,它会走到MyRealm.doGetAuthenticationInfo()方法中,具体验证方式详见此方法
            logger.info("对用户[" + userName + "]进行登录验证..验证开始");
            currentUser.login(token);
            logger.info("对用户[" + userName + "]进行登录验证..验证通过");
        }
        catch (UnknownAccountException uae)
        {
            logger.info("对用户[" + userName + "]进行登录验证..验证未通过,未知账户");
            redirectAttributes.addFlashAttribute("message", "未知账户");
            result = "用户名或密码不正确";
        }
        catch (IncorrectCredentialsException ice)
        {
            logger.info("对用户[" + userName + "]进行登录验证..验证未通过,错误的凭证");
            redirectAttributes.addFlashAttribute("message", "密码不正确");
            result = "用户名或密码不正确";
        }
        catch (LockedAccountException lae)
        {
            logger.info("对用户[" + userName + "]进行登录验证..验证未通过,账户已锁定");
            redirectAttributes.addFlashAttribute("message", "账户已锁定");
            result = "账户已被锁定";
        }
        catch (ExcessiveAttemptsException eae)
        {
            logger.info("对用户[" + userName + "]进行登录验证..验证未通过,错误次数过多");
            redirectAttributes.addFlashAttribute("message", "用户名或密码错误次数过多");
            result = "错误次数过多,请稍后再试";
        }
        catch (AuthenticationException ae)
        {
            //通过处理Shiro的运行时AuthenticationException就可以控制用户登录失败或密码错误时的情景
            logger.info("对用户[" + userName + "]进行登录验证..验证未通过,堆栈轨迹如下");
            ae.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "用户名或密码不正确");
            result = "用户名或密码不正确";
        }

        //验证是否登录成功
        if (currentUser.isAuthenticated())
        {
            logger.info("用户[" + userName + "]登录认证通过(这里可以进行一些认证通过后的一些系统参数初始化操作)");
            return "1";
        }
        else
        {
            token.clear();
            //return "redirect:/login";
            return result;
        }
    }

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logout(RedirectAttributes redirectAttributes)
    {
        //使用权限管理工具进行用户的退出，跳出登录，给出提示信息
        SecurityUtils.getSubject().logout();
        //redirectAttributes.addFlashAttribute("message", "您已安全退出");
        return "redirect:/login";
    }

    @RequestMapping("/403")
    public String unauthorizedRole()
    {
        logger.info("------没有权限-------");
        return "403";
    }
}
