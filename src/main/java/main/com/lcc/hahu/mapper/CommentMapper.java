package main.com.lcc.hahu.mapper;


import java.util.List;

import main.com.lcc.hahu.model.AnswerComment;
import main.com.lcc.hahu.model.QuestionComment;
import org.apache.ibatis.annotations.Param;

public interface CommentMapper {

    List<QuestionComment> listQuestionCommentByQuestionId(@Param("questionId") Integer questionId);

    List<AnswerComment> listAnswerCommentByAnswerId(@Param("answerId") Integer answerId);

    void insertQuestionComment(QuestionComment comment);

    void insertQuestionCommentReply(QuestionComment comment);

    void insertAnswerComment(AnswerComment comment);

    void insertAnswerCommentReply(AnswerComment comment);

    int selectAnswerCommentCountByAnswerId(@Param("answerId") Integer answerId);

}
