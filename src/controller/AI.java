package controller;

import components.ChessGridComponent;
import model.ChessPiece;
import model.Step;
import model.UndoList;
import view.ChessBoardPanel;

import static view.ChessBoardPanel.CHESS_COUNT;

public class AI {
    private static final int INF = 2147483647;
    private static ChessGridComponent[][] chessGrids;
    private static ChessBoardPanel panel;
    private static int[][] panelScore;//棋盘上的权值
    private static Step[][][] history;
    public static int dx,dy;

    public static void setPanel(ChessBoardPanel panel) {
        AI.panel = panel;
        AI.chessGrids = panel.getChessGrids();
    }
    public static void advantageMove(int u,int depth,int index){
        if(index==0) return;
        Step v=history[u][depth][index];
        history[u][depth][index]=history[u][depth][index-1];
        history[u][depth][index-1]=v;
    }
    private static void floatMove(int u,int depth,int index){
        Step s=history[u][depth][index];
        for(int i=index-1;i>=0;i--){
            history[u][depth][i+1]=history[u][depth][i];
        }
        history[u][depth][0]=s;
    }

    public static void initScore() {
        panelScore = new int[9][9];
        history=new Step[2][70][64];
        for (int i = 0; i < 70; i++) {
            for (int j = 0; j < 64; j++) {
                history[0][i][j] = new Step(j / 8, j % 8, ChessPiece.BLACK, null);
                history[1][i][j] = new Step(j / 8, j % 8, ChessPiece.BLACK, null);
            }
        }

        for(int i=0;i<8;i++){
            for(int j=0;j<8;j++){
                if(i*(7-i)==0&&j*(7-j)==0)//在角
                    panelScore[i][j]=64;
                else if((i<=1||i>=6)&&(j<=1||j>=6))//在角四周
                    panelScore[i][j]=0;
                else if((i-1)*(j-1)*(6-i)*(6-j)==0)//在第二行
                    panelScore[i][j]=4;
                else if(i*j*(7-i)*(7-j)==0)//在边
                    panelScore[i][j]=6;
                else//在中间
                    panelScore[i][j]=2;
            }
        }
    }

    /**
     * AI调用入口，函数结束会调用某个格子的onMouseClicked来模拟点击
     * AI思路：遍历所有能下的点，假如我下这里，那么搜索下一个玩家继续移动最优的值，找到这个值的最小，就是我们移动的地方
     * 值：指与对家的得分之差
     * 最优：指机器认为最优，也就是与对家得分之差最大
     * <p>
     * 这个方法的问题在于，因为搜索本身限制，一次递归需遍历64个点，最多时进入10个递归
     * 搜索n次则复杂度 O(64*10^n)
     * 所以只能搜索6~8步，也就3 4回合，
     * 搜索权值为棋盘权重
     *
     * @param level AI等级，也就是搜索深度
     */
    public static void AIPlay(int level, ChessPiece currentPlayer) {
        panel.checkPlaceable(currentPlayer);
        ChessGridComponent[][] chessGrids = panel.getChessGrids();
        int u = 0;
        for (int i = 0; i < CHESS_COUNT; i++) {
            for (int j = 0; j < CHESS_COUNT; j++) {
                if (chessGrids[i][j].getChessPiece() != ChessPiece.GRAY && chessGrids[i][j].getChessPiece() != null) {
                    u++;
                }
            }
        }
        dx=dy=-1;
        int tmp = -(54 < u
                    ? think(u, 64 - u, currentPlayer, false, currentPlayer,-INF,INF)
                    : think(u, level, currentPlayer, true, currentPlayer,-INF,INF));

        ChessGridComponent.AIOn = false;
        System.out.println("AIOn=false\n");
        panel.checkPlaceable(currentPlayer);
        if (dx == -1) {
            panel.doJump();
        } else {
            System.out.printf("AI play at %d,%d\n", dx, dy);
            chessGrids[dx][dy].onMouseClicked();
        }
    }

    /**
     * 这里就是递归搜索
     */
    private static int think(int depth, int level, ChessPiece currentPlayer, boolean enableScore, ChessPiece AIPiece,int alpha,int beta) {

        int max=-2147483647;
        if (level == 0){
            return evaluateBoard(AIPiece, currentPlayer, enableScore);
        }
        int nn = currentPlayer == ChessPiece.BLACK ? 1 : 0, v;
        int nx=-1,ny = -1;
        ChessPiece nCur = currentPlayer == ChessPiece.BLACK ? ChessPiece.WHITE : ChessPiece.BLACK;
        for (int T = 0; T < 64; T++) {
            Step s = history[nn][depth][T];
            int i = s.rowIndex, j = s.columnIndex;
            if (checkPlaceable(i,j,currentPlayer)) {
                panel.doMove(i,j,currentPlayer);
                panel.getUndoList().add(i, j, currentPlayer);

                v=-think(depth+1,level-1,nCur,enableScore,AIPiece,-beta,-alpha);

                panel.getUndoList().undo(chessGrids);

                if(v>alpha){
                    if(v>beta){
                        dx=i;dy=j;
                        floatMove(nn,depth,T);
                        return v;
                    }
                    advantageMove(nn,depth,T);
                    alpha=v;
                }
                if(max<v){
                    nx=i;ny=j;
                    max=v;
                }
            }
        }
        dx=nx;dy=ny;
        return max;
    }

    private static boolean checkPlaceable(int i, int j, ChessPiece currentPlayer) {
        if(chessGrids[i][j].getChessPiece()==ChessPiece.BLACK || chessGrids[i][j].getChessPiece() == ChessPiece.WHITE) return false;
        for(int T=0;T<8;T++){
            if(panel.canPut(i+ UndoList.xDirection[T],j+ UndoList.yDirection[T],T,currentPlayer,false)) return true;
        }
        return false;
    }

    private static int evaluateBoard(ChessPiece AIPiece, ChessPiece currentPlayer, boolean enableScore) {
        int dif = 0;

        for (int i = 0; i < CHESS_COUNT; i++) {
            for (int j = 0; j < CHESS_COUNT; j++) {
                if (chessGrids[i][j].getChessPiece() == AIPiece) {
                    if (enableScore) dif += panelScore[i][j];
                    else dif++;
                } else if (chessGrids[i][j].getChessPiece() == (AIPiece == ChessPiece.BLACK ? ChessPiece.WHITE : ChessPiece.BLACK)) {
                    if (enableScore) dif -= panelScore[i][j];
                    else dif--;
                }
            }
        }
        if (enableScore) {
            if (AIPiece == currentPlayer) {
                dif += panel.countGray() * 5;
                panel.checkPlaceable(AIPiece == ChessPiece.BLACK ? ChessPiece.WHITE : ChessPiece.BLACK);
                dif -= panel.countGray() * 5;
            } else {
                dif -= panel.countGray() * 5;
                panel.checkPlaceable(AIPiece);
                dif += panel.countGray() * 5;
            }
        }
        return dif*(AIPiece==currentPlayer ? 1 : -1);
    }

    /**
     * 输出棋盘状态
     */
    private static void opt(ChessGridComponent[][] chessGrids) {
        for (int i = 0; i < CHESS_COUNT; i++) {
            for (int j = 0; j < CHESS_COUNT; j++) {
                if(chessGrids[i][j].getChessPiece()== ChessPiece.GRAY){
                    System.out.print("G");
                }else if(chessGrids[i][j].getChessPiece()== ChessPiece.BLACK){
                    System.out.print("B");
                }else if(chessGrids[i][j].getChessPiece()== ChessPiece.WHITE) {
                    System.out.print("W");
                }else System.out.print(" ");
            }
            System.out.print('\n');
        }
    }
}
