package main.com.lcc.hahu.controller;

import main.com.lcc.hahu.model.AnswerComment;
import main.com.lcc.hahu.model.QuestionComment;
import main.com.lcc.hahu.service.CommentService;
import main.com.lcc.hahu.service.UserService;
import main.com.lcc.hahu.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by liangchengcheng on 2017/7/9.
 */

@Controller
@RequestMapping("/")
public class CommentController {

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @RequestMapping("/commentQuestion")
    @ResponseBody
    public Response commentQuestion(Integer questionId, String commentContent, HttpServletRequest request) {
        Integer userId = userService.getUserIdFromRedis(request);
        QuestionComment comment = commentService.commentQuestion(questionId, commentContent, userId);
        return new Response(0, "", comment);
    }

    @RequestMapping("/replyQuestionComment")
    @ResponseBody
    public Response replyQuestionComment(QuestionComment questionComment, HttpServletRequest request) {
        System.out.println(questionComment);
        Integer userId = userService.getUserIdFromRedis(request);
        QuestionComment comment = commentService.replyQuestionComment(questionComment, userId);
        return new Response(0, "", comment);
    }

    @RequestMapping("/commentAnswer")
    @ResponseBody
    public Response commentAnswer(Integer answerId, String commentContent, HttpServletRequest request) {
        Integer userId = userService.getUserIdFromRedis(request);
        AnswerComment comment = commentService.commentAnswer(answerId, commentContent, userId);
        return new Response(0, "", comment);
    }

    @RequestMapping("/replyAnswerComment")
    @ResponseBody
    public Response replyAnswerComment(AnswerComment answerComment, HttpServletRequest request) {
        System.out.println(answerComment);
        Integer userId = userService.getUserIdFromRedis(request);
        AnswerComment comment = commentService.replyAnswerComment(answerComment, userId);
        return new Response(0, "", comment);
    }

    @RequestMapping("/likeQuestionComment")
    @ResponseBody
    public Response likeQuestionComment(Integer questionCommentId, HttpServletRequest request) {
        Integer userId = userService.getUserIdFromRedis(request);
        commentService.likeQuestionComment(userId, questionCommentId);
        return new Response(0, "");
    }

    @RequestMapping("/likeAnswerComment")
    @ResponseBody
    public Response likeAnswerComment(Integer answerCommentId, HttpServletRequest request) {
        Integer userId = userService.getUserIdFromRedis(request);
        commentService.likeAnswerComment(userId, answerCommentId);
        return new Response(0, "");
    }
}
