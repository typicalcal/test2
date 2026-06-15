import java.util.Scanner;

public class testTTT {
    public static void main(String[] args) {
        int turn = 0;
        int moveCount = 0;
        char[][] board = new char[3][3];
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                board[row][col] = ' ';
            }
        }

        Scanner scanner = new Scanner(System.in);

        while (true) {
            printBoard(board);
            playerMove(board, turn, scanner);
            moveCount++;

            if (moveCount > 4 && checkGame(board)) {
                printBoard(board);
                char symbol = (turn == 0) ? 'O' : 'X';
                System.out.println("Player " + symbol + " wins!");
                break;
            }

            if (moveCount == 9) {
                printBoard(board);
                System.out.println("It's a draw!");
                break;
            }

            turn = 1 - turn;
        }
    }

    public static boolean checkGame(char[][] board) {
        for (int i = 0; i < 3; i++) {
            if (board[i][0] != ' ' && board[i][0] == board[i][1] && board[i][1] == board[i][2]) {
                return true;
            }
            if (board[0][i] != ' ' && board[0][i] == board[1][i] && board[1][i] == board[2][i]) {
                return true;
            }
        }

        if (board[0][0] != ' ' && board[0][0] == board[1][1] && board[1][1] == board[2][2]) {
            return true;
        }
        if (board[0][2] != ' ' && board[0][2] == board[1][1] && board[1][1] == board[2][0]) {
            return true;
        }

        return false;
    }

    public static void playerMove(char[][] board, int turn, Scanner scanner) {
        char symbol = (turn == 0) ? 'O' : 'X';

        while (true) {
            System.out.println("Player " + symbol + ", enter row and column (0-2):");
            int row = scanner.nextInt();
            int col = scanner.nextInt();

            if (row < 0 || row > 2 || col < 0 || col > 2) {
                System.out.println("Row and column must be between 0 and 2.");
            } else if (board[row][col] != ' ') {
                System.out.println("That spot is taken, choose another move.");
            } else {
                board[row][col] = symbol;
                break;
            }
        }
    }

    public static void printBoard(char[][] board) {
        for (int row = 0; row < 3; row++) {
            System.out.println(board[row][0] + " | " + board[row][1] + " | " + board[row][2]);
            if (row < 2) {
                System.out.println("---------");
            }
        }
    }
}
