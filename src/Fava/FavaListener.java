// Generated from C:/Users/pedro/OneDrive/Ambiente de Trabalho/Compiladores/Projeto3/Fava.g4 by ANTLR 4.13.2
package Fava;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link FavaParser}.
 */
public interface FavaListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link FavaParser#prog}.
	 * @param ctx the parse tree
	 */
	void enterProg(FavaParser.ProgContext ctx);
	/**
	 * Exit a parse tree produced by {@link FavaParser#prog}.
	 * @param ctx the parse tree
	 */
	void exitProg(FavaParser.ProgContext ctx);
	/**
	 * Enter a parse tree produced by {@link FavaParser#globalDecl}.
	 * @param ctx the parse tree
	 */
	void enterGlobalDecl(FavaParser.GlobalDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link FavaParser#globalDecl}.
	 * @param ctx the parse tree
	 */
	void exitGlobalDecl(FavaParser.GlobalDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link FavaParser#funcDecl}.
	 * @param ctx the parse tree
	 */
	void enterFuncDecl(FavaParser.FuncDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link FavaParser#funcDecl}.
	 * @param ctx the parse tree
	 */
	void exitFuncDecl(FavaParser.FuncDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link FavaParser#paramList}.
	 * @param ctx the parse tree
	 */
	void enterParamList(FavaParser.ParamListContext ctx);
	/**
	 * Exit a parse tree produced by {@link FavaParser#paramList}.
	 * @param ctx the parse tree
	 */
	void exitParamList(FavaParser.ParamListContext ctx);
	/**
	 * Enter a parse tree produced by {@link FavaParser#param}.
	 * @param ctx the parse tree
	 */
	void enterParam(FavaParser.ParamContext ctx);
	/**
	 * Exit a parse tree produced by {@link FavaParser#param}.
	 * @param ctx the parse tree
	 */
	void exitParam(FavaParser.ParamContext ctx);
	/**
	 * Enter a parse tree produced by {@link FavaParser#returnType}.
	 * @param ctx the parse tree
	 */
	void enterReturnType(FavaParser.ReturnTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link FavaParser#returnType}.
	 * @param ctx the parse tree
	 */
	void exitReturnType(FavaParser.ReturnTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link FavaParser#declItem}.
	 * @param ctx the parse tree
	 */
	void enterDeclItem(FavaParser.DeclItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link FavaParser#declItem}.
	 * @param ctx the parse tree
	 */
	void exitDeclItem(FavaParser.DeclItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link FavaParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(FavaParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link FavaParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(FavaParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link FavaParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(FavaParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link FavaParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(FavaParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link FavaParser#localDecl}.
	 * @param ctx the parse tree
	 */
	void enterLocalDecl(FavaParser.LocalDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link FavaParser#localDecl}.
	 * @param ctx the parse tree
	 */
	void exitLocalDecl(FavaParser.LocalDeclContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PrintStat}
	 * labeled alternative in {@link FavaParser#stat}.
	 * @param ctx the parse tree
	 */
	void enterPrintStat(FavaParser.PrintStatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PrintStat}
	 * labeled alternative in {@link FavaParser#stat}.
	 * @param ctx the parse tree
	 */
	void exitPrintStat(FavaParser.PrintStatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code AssignStat}
	 * labeled alternative in {@link FavaParser#stat}.
	 * @param ctx the parse tree
	 */
	void enterAssignStat(FavaParser.AssignStatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code AssignStat}
	 * labeled alternative in {@link FavaParser#stat}.
	 * @param ctx the parse tree
	 */
	void exitAssignStat(FavaParser.AssignStatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BlockStat}
	 * labeled alternative in {@link FavaParser#stat}.
	 * @param ctx the parse tree
	 */
	void enterBlockStat(FavaParser.BlockStatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BlockStat}
	 * labeled alternative in {@link FavaParser#stat}.
	 * @param ctx the parse tree
	 */
	void exitBlockStat(FavaParser.BlockStatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code WhileStat}
	 * labeled alternative in {@link FavaParser#stat}.
	 * @param ctx the parse tree
	 */
	void enterWhileStat(FavaParser.WhileStatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code WhileStat}
	 * labeled alternative in {@link FavaParser#stat}.
	 * @param ctx the parse tree
	 */
	void exitWhileStat(FavaParser.WhileStatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IfElseStat}
	 * labeled alternative in {@link FavaParser#stat}.
	 * @param ctx the parse tree
	 */
	void enterIfElseStat(FavaParser.IfElseStatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IfElseStat}
	 * labeled alternative in {@link FavaParser#stat}.
	 * @param ctx the parse tree
	 */
	void exitIfElseStat(FavaParser.IfElseStatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IfStat}
	 * labeled alternative in {@link FavaParser#stat}.
	 * @param ctx the parse tree
	 */
	void enterIfStat(FavaParser.IfStatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IfStat}
	 * labeled alternative in {@link FavaParser#stat}.
	 * @param ctx the parse tree
	 */
	void exitIfStat(FavaParser.IfStatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ReturnStat}
	 * labeled alternative in {@link FavaParser#stat}.
	 * @param ctx the parse tree
	 */
	void enterReturnStat(FavaParser.ReturnStatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ReturnStat}
	 * labeled alternative in {@link FavaParser#stat}.
	 * @param ctx the parse tree
	 */
	void exitReturnStat(FavaParser.ReturnStatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CallStat}
	 * labeled alternative in {@link FavaParser#stat}.
	 * @param ctx the parse tree
	 */
	void enterCallStat(FavaParser.CallStatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CallStat}
	 * labeled alternative in {@link FavaParser#stat}.
	 * @param ctx the parse tree
	 */
	void exitCallStat(FavaParser.CallStatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code EmptyStat}
	 * labeled alternative in {@link FavaParser#stat}.
	 * @param ctx the parse tree
	 */
	void enterEmptyStat(FavaParser.EmptyStatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code EmptyStat}
	 * labeled alternative in {@link FavaParser#stat}.
	 * @param ctx the parse tree
	 */
	void exitEmptyStat(FavaParser.EmptyStatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code AndExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterAndExpr(FavaParser.AndExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code AndExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitAndExpr(FavaParser.AndExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ConcatExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterConcatExpr(FavaParser.ConcatExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ConcatExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitConcatExpr(FavaParser.ConcatExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MultDivModExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterMultDivModExpr(FavaParser.MultDivModExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MultDivModExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitMultDivModExpr(FavaParser.MultDivModExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IdExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterIdExpr(FavaParser.IdExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IdExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitIdExpr(FavaParser.IdExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code RelExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterRelExpr(FavaParser.RelExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code RelExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitRelExpr(FavaParser.RelExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code EqExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterEqExpr(FavaParser.EqExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code EqExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitEqExpr(FavaParser.EqExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LiteralExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterLiteralExpr(FavaParser.LiteralExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LiteralExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitLiteralExpr(FavaParser.LiteralExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CallExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterCallExpr(FavaParser.CallExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CallExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitCallExpr(FavaParser.CallExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ParenExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterParenExpr(FavaParser.ParenExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ParenExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitParenExpr(FavaParser.ParenExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code UnaryExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpr(FavaParser.UnaryExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code UnaryExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpr(FavaParser.UnaryExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code AddSubExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterAddSubExpr(FavaParser.AddSubExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code AddSubExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitAddSubExpr(FavaParser.AddSubExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code OrExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterOrExpr(FavaParser.OrExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code OrExpr}
	 * labeled alternative in {@link FavaParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitOrExpr(FavaParser.OrExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link FavaParser#funcCall}.
	 * @param ctx the parse tree
	 */
	void enterFuncCall(FavaParser.FuncCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link FavaParser#funcCall}.
	 * @param ctx the parse tree
	 */
	void exitFuncCall(FavaParser.FuncCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link FavaParser#argList}.
	 * @param ctx the parse tree
	 */
	void enterArgList(FavaParser.ArgListContext ctx);
	/**
	 * Exit a parse tree produced by {@link FavaParser#argList}.
	 * @param ctx the parse tree
	 */
	void exitArgList(FavaParser.ArgListContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IntLiteral}
	 * labeled alternative in {@link FavaParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterIntLiteral(FavaParser.IntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IntLiteral}
	 * labeled alternative in {@link FavaParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitIntLiteral(FavaParser.IntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code RealLiteral}
	 * labeled alternative in {@link FavaParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterRealLiteral(FavaParser.RealLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code RealLiteral}
	 * labeled alternative in {@link FavaParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitRealLiteral(FavaParser.RealLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code StringLiteral}
	 * labeled alternative in {@link FavaParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(FavaParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code StringLiteral}
	 * labeled alternative in {@link FavaParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(FavaParser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TrueLiteral}
	 * labeled alternative in {@link FavaParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterTrueLiteral(FavaParser.TrueLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TrueLiteral}
	 * labeled alternative in {@link FavaParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitTrueLiteral(FavaParser.TrueLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FalseLiteral}
	 * labeled alternative in {@link FavaParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterFalseLiteral(FavaParser.FalseLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FalseLiteral}
	 * labeled alternative in {@link FavaParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitFalseLiteral(FavaParser.FalseLiteralContext ctx);
}