package view;

import components.ChessGridComponent;
import controller.AI;
import controller.GameController;
import controller.Trainer;
import model.ChessPiece;
import model.UndoList;

import javax.swing.*;
import java.awt.*;

public class ChessBoardPanel extends JPanel {
    public static final int CHESS_COUNT = 8;
    private ChessGridComponent[][] chessGrids;
    UndoList undoList;

    public ChessBoardPanel(int width, int height) {
        this.setVisible(true);
        this.setFocusable(true);
        this.setLayout(null);
        this.setBackground(Color.BLACK);
        int length = Math.min(width, height);
        this.setSize(length, length);
        ChessGridComponent.gridSize = length / CHESS_COUNT;
        ChessGridComponent.chessSize = (int) (ChessGridComponent.gridSize * 0.8);
        System.out.printf("width = %d height = %d gridSize = %d chessSize = %d\n",
                width, height, ChessGridComponent.gridSize, ChessGridComponent.chessSize);

        initialChessGrids();//return empty chessboard
        initialGame();//add initial four chess
        undoList = new UndoList();
        AI.setPanel(this);
        AI.initScore();

        repaint();
    }

    /**
     * set an empty chessboard
     */
    public void initialChessGrids() {
        chessGrids = new ChessGridComponent[CHESS_COUNT][CHESS_COUNT];

        //draw all chess grids
        for (int i = 0; i < CHESS_COUNT; i++) {
            for (int j = 0; j < CHESS_COUNT; j++) {
                ChessGridComponent gridComponent = new ChessGridComponent(i, j);
                gridComponent.setLocation(j * ChessGridComponent.gridSize, i * ChessGridComponent.gridSize);
                chessGrids[i][j] = gridComponent;
                this.add(chessGrids[i][j]);
            }
        }
    }

    /**
     * initial origin four chess
     */
    public void initialGame() {
        System.out.println("initialing...");
        for (int i = 0; i < CHESS_COUNT; i++) {
            for (int j = 0; j < CHESS_COUNT; j++) {
                chessGrids[i][j].setChessPiece(null);
            }
        }
        ChessGridComponent.setLast(-1, -1);
        chessGrids[3][3].setChessPiece(ChessPiece.BLACK);
        chessGrids[3][4].setChessPiece(ChessPiece.WHITE);
        chessGrids[4][3].setChessPiece(ChessPiece.WHITE);
        chessGrids[4][4].setChessPiece(ChessPiece.BLACK);

        repaint();
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(GameFrame.getPanelImage(), 0, 0, this.getWidth(), this.getHeight(), null);
    }

    /**
     * ???????????????????????????????????????
     */
    public boolean canClickGrid(int row, int col) {
        return GameFrame.cheat || chessGrids[row][col].getChessPiece() == ChessPiece.GRAY;
        //return true;
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param currentPlayer ??????????????????
     */
    public void checkPlaceable(ChessPiece currentPlayer, ChessGridComponent[][] chessGrids) {
        if (chessGrids == null) chessGrids = this.chessGrids;
        for (int i = 0; i < CHESS_COUNT; i++) {//????????????????????????????????????????????????????????????
            for (int j = 0; j < CHESS_COUNT; j++) {
                if (chessGrids[i][j].getChessPiece() == ChessPiece.GRAY)
                    chessGrids[i][j].setChessPiece(null);
            }
        }
        for (int i = 0; i < CHESS_COUNT; i++) {//????????????????????????????????????8??????????????????????????????
            for (int j = 0; j < CHESS_COUNT; j++) {
                if (chessGrids[i][j].getChessPiece() == currentPlayer) {
                    findPut(i, j, currentPlayer, chessGrids);
                }
            }
        }
    }

    /**
     * ???????????????????????????????????????
     */
    private boolean checkBounder(int row, int col) {
        return (0 <= row && row < CHESS_COUNT) && (0 <= col && col < CHESS_COUNT);
    }

    /**
     * ?????????dx,dy?????????T?????????????????????????????????  T?????????initialDirection??????
     *
     * @param ckOnly ????????????????????????????????????????????????????????????????????????????????????
     *               ?????????????????????????????????????????????
     */
    public boolean canPut(int dx, int dy, int T, ChessPiece currentPlayer, boolean ckOnly, ChessGridComponent[][] chessGrids) {
        if (chessGrids == null) chessGrids = this.chessGrids;
        int cnt = 0;
        while (checkBounder(dx, dy)) {
            if (chessGrids[dx][dy].getChessPiece() == null || chessGrids[dx][dy].getChessPiece() == ChessPiece.GRAY) {
                if (cnt == 0) {
                    break;
                }
                if (ckOnly) {
                    chessGrids[dx][dy].setChessPiece(ChessPiece.GRAY);
                    return true;
                } else return false;
            } else if (chessGrids[dx][dy].getChessPiece() == currentPlayer) {
                if (ckOnly) break;
                else {
                    return cnt != 0;
                }
            } else {
                cnt++;
            }
            dx = dx + UndoList.xDirection[T];
            dy = dy + UndoList.yDirection[T];
        }
        return false;
    }

    /**
     * ???checkPlaceable?????????????????????????????????????????????8?????????????????????
     */

    private void findPut(int row, int col, ChessPiece currentPlayer, ChessGridComponent[][] chessGrids) {
        for (int T = 0; T < UndoList.directionCounter; T++) {
            int dx = row + UndoList.xDirection[T];
            int dy = col + UndoList.yDirection[T];
            canPut(dx, dy, T, currentPlayer, true, chessGrids);
        }
    }

    /**
     * ??????????????????
     *
     * @return ??????????????????????????????
     */
    public int doMove(int row, int col, ChessPiece currentPlayer, ChessGridComponent[][] chessGrids) {
        if (chessGrids == null) chessGrids = this.chessGrids;
        int t = 0;
        for (int T = 0; T < UndoList.directionCounter; T++) {
            int dx = row + UndoList.xDirection[T];
            int dy = col + UndoList.yDirection[T];
            undoList.setReserveNum(T, 0);
            if (canPut(dx, dy, T, currentPlayer, false, chessGrids)) {
                while (chessGrids[dx][dy].getChessPiece() != currentPlayer) {
                    chessGrids[dx][dy].setChessPiece(currentPlayer);
                    t++;
                    undoList.addReserveNum(T, 1);
                    dx += UndoList.xDirection[T];
                    dy += UndoList.yDirection[T];
                }
            }
        }
        chessGrids[row][col].setChessPiece(currentPlayer);
        return t + 1;
    }


    public void doJump() {
        if (!Trainer.on)
            JOptionPane.showMessageDialog(GameFrame.controller.getGamePanel(),
                    (GameFrame.controller.getCurrentPlayer() == ChessPiece.BLACK ? "BLACK" : "WHITE") +
                            " has nowhere to put! JumpThrough.");
        GameFrame.controller.jumpThrough();
        if (GameFrame.controller.getGamePanel().checkGray()) {//????????????
            if (!Trainer.on)
                JOptionPane.showMessageDialog(GameFrame.controller.getGamePanel(),
                        (GameFrame.controller.getCurrentPlayer() == ChessPiece.BLACK ? "BLACK" : "WHITE") +
                                " has nowhere to put! JumpThrough.");
            GameFrame.controller.jumpThrough();
        }
        checkPlaceable(GameFrame.controller.getCurrentPlayer(), null);
        repaint();
    }


    /**
     * ????????????????????????
     */
    public int countGray() {
        int u = 0;
        for (int i = 0; i < CHESS_COUNT; i++) {
            for (int j = 0; j < CHESS_COUNT; j++) {
                if (chessGrids[i][j].getChessPiece() == ChessPiece.GRAY)
                    u++;
            }
        }
        return u;
    }

    /**
     * ???????????????????????????????????????????????????????????????
     */
    public boolean checkGray() {
        for (int i = 0; i < CHESS_COUNT; i++) {
            for (int j = 0; j < CHESS_COUNT; j++) {
                if (((GameFrame.controller.getCurrentPlayer() != GameFrame.AIPiece && GameFrame.cheat) || (GameFrame.controller.getCurrentPlayer() == GameFrame.AIPiece && GameFrame.AICheat))
                        && chessGrids[i][j].getChessPiece() == null) return false;
                if (chessGrids[i][j].getChessPiece() == ChessPiece.GRAY) return false;
            }
        }
        return true;
    }

    public UndoList getUndoList() {
        return undoList;
    }

    /**
     * ???????????????
     *
     * @return ???????????????
     */
    public int undo() {
        return undoList.undo(chessGrids);
    }

    public boolean hasNextUndo() {
        return undoList.hasNext();
    }

    public void addUndo(int row, int col, ChessPiece currentPlayer) {
        undoList.add(row, col, currentPlayer);
    }

    /**
     * ?????????????????????????????????????????????
     */
    public void doUndo() {
        ChessPiece cur;
        int u;
        GameController controller = GameFrame.controller;
        while (controller.getGamePanel().hasNextUndo() && GameFrame.AIPiece == controller.getGamePanel().getUndoList().getLastColor()) {
            cur = GameFrame.AIPiece;
            undoWithScore(cur, controller);
        }
        cur = controller.getGamePanel().getUndoList().getLastColor();
        undoWithScore(cur, controller);
        controller.getGamePanel().repaint();
    }

    private void undoWithScore(ChessPiece cur, GameController controller) {
        int u;
        u = undo();
        controller.countScore(cur == ChessPiece.BLACK ? ChessPiece.WHITE : ChessPiece.BLACK, u);
        controller.countScore(cur, -u - 1);
        if (cur != controller.getCurrentPlayer())
            controller.swapPlayer();
        else
            controller.getGamePanel().checkPlaceable(controller.getCurrentPlayer(), null);
        System.out.println(u);
    }

    public ChessGridComponent[][] getChessGrids() {
        return chessGrids;
    }


    /**
     * XY?????????
     */
    public void flipX() {
        ChessPiece s;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 4; j++) {
                s = chessGrids[i][j].getChessPiece();
                chessGrids[i][j].setChessPiece(chessGrids[i][7 - j].getChessPiece());
                chessGrids[i][7 - j].setChessPiece(s);
            }
        }
        ChessGridComponent.setLast(ChessGridComponent.getLastRow(), 7 - ChessGridComponent.getLastCol());
        repaint();
    }

    public void flipY() {
        ChessPiece s;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 8; j++) {
                s = chessGrids[i][j].getChessPiece();
                chessGrids[i][j].setChessPiece(chessGrids[7 - i][j].getChessPiece());
                chessGrids[7 - i][j].setChessPiece(s);
            }
        }
        ChessGridComponent.setLast(7 - ChessGridComponent.getLastRow(), ChessGridComponent.getLastCol());
        repaint();
    }

    /**
     * ?????????????????????
     */
    public void reSize(int width, int height) {
        int length = Math.min(width, height);
        this.setSize(length, length);
        ChessGridComponent.gridSize = (int) ((length + 0.5) / CHESS_COUNT);
        double gridSize = length * 1.0 / CHESS_COUNT;
        ChessGridComponent.chessSize = (int) (ChessGridComponent.gridSize * 0.8);

        for (int i = 0; i < CHESS_COUNT; i++) {
            for (int j = 0; j < CHESS_COUNT; j++) {
                chessGrids[i][j].resize(ChessGridComponent.gridSize);
                chessGrids[i][j].setLocation((int) (j * gridSize + 0.5), (int) (i * gridSize + 0.5));
            }
        }
    }

    @Override
    public String toString() {
        ChessPiece cs;
        String tostr = String.format("%d %d \r\n", CHESS_COUNT, CHESS_COUNT);
        for (int i = 0; i < CHESS_COUNT; i++) {
            for (int j = 0; j < CHESS_COUNT; j++) {
                cs = chessGrids[i][j].getChessPiece();
                if (cs == null) tostr = String.format("%s0 ", tostr);
                else tostr = String.format("%s%s ", tostr, cs.toString());
            }
            tostr = String.format("%s\r\n", tostr);
        }
        return String.format("%s%s", tostr, undoList.toString());
    }
}
