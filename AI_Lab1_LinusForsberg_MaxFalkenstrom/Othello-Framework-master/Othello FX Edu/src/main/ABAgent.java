package main;

import com.eudycontreras.othello.capsules.AgentMove;
import com.eudycontreras.othello.capsules.MoveWrapper;
import com.eudycontreras.othello.capsules.ObjectiveWrapper;
import com.eudycontreras.othello.controllers.Agent;
import com.eudycontreras.othello.controllers.AgentController;
import com.eudycontreras.othello.enumerations.BoardCellState;
import com.eudycontreras.othello.enumerations.PlayerTurn;
import com.eudycontreras.othello.models.GameBoardState;
import java.util.List;

public class ABAgent extends Agent {

    private long startTime;
    private int depthLimit = 5;
    private int startCount = 0;

    public ABAgent(String name, PlayerTurn playerTurn) {
       super(name, playerTurn);
    }

    @Override
    public AgentMove getMove(GameBoardState gameState) {
        startTime = System.currentTimeMillis(); // används för att mäta tiden för cut off
        startCount = (int) gameState.getGameBoard().getCount(BoardCellState.ANY); // antal brickor som ligger på brädet när metoden kallas
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;


        int v = Integer.MIN_VALUE; // används för att hitta högst utility
        int index = -1; // index används för att hålla koll på vilket move som är bäst och ska returneras
        List<ObjectiveWrapper> availableMoves = AgentController.getAvailableMoves(gameState, playerTurn); // lagliga drag

        // förenklad max value, går igenom alla möjliga drag och anropar minValue som kör igång processen att hitta bäst utility value
        for(int i = 0; i < availableMoves.size(); i++) {
            GameBoardState gbs = AgentController.getNewState(gameState, availableMoves.get(i)); // kopierar state för hur nästa move skulle se ut om vi gör det draget
            int utility = minValue(gbs, alpha, beta);

            // sätter v till utility om nytt bästa har hittats och byter ut index mot nytt värde
            if(utility > v) {
                v = utility;
                index = i;
            }
        }

        return index != -1 ? new MoveWrapper(availableMoves.get(index)) : null; // returnerar bäst move eller null om inget move finns
    }

    public int maxValue(GameBoardState gameState, int alpha, int beta) {
        nodesExamined++; // håller reda på antal noder som sökts igenom
        int currentDepth = (int)gameState.getGameBoard().getCount(BoardCellState.ANY) - startCount; // sökdjupet

        if(currentDepth > searchDepth) searchDepth = currentDepth; // om sökdjupet har ökats så ökas currentdepth

        if(AgentController.isTerminal(gameState, playerTurn)) { // om lövnod har hittats
            reachedLeafNodes++;
            return getUtility(gameState);
        }

        // kollar så att man inte sökt för djupt eller för länge. Antingen 5 sek, eller sökdjupet är större än djupgränsen och 35% av tiden passerat
        if(AgentController.cutOffTest(UserSettings.MAX_SEARCH_TIME,searchDepth,depthLimit,startTime)) {
            return getUtility(gameState);
        }

        int v = Integer.MIN_VALUE; // används för att hitta högst utility
        List<ObjectiveWrapper> availableMoves = AgentController.getAvailableMoves(gameState, playerTurn); // tillgängliga moves

        // går igenom alla möjliga drag och anropar minValue
        for(int i = 0; i < availableMoves.size(); i++) {
            GameBoardState gbs = AgentController.getNewState(gameState, availableMoves.get(i));  // kopierar state för hur nästa move skulle se ut om vi gör det draget
            int utility = minValue(gbs, alpha, beta); // anropa minValue metoden för nästa game state

            if(utility > v) v = utility; // om högre utility värde hittats byts värdet ut

            if(v >= beta) { // om v större än beta
                prunedCounter += availableMoves.size() - 1 - i; // lägger till antalet prunade noder
                return v; // prunar resterande
            }

            if(v > alpha) alpha = v; // om bättre alpha värde hittas byts värdet ut
        }
        return v; // returnerar utility värde
    }

    public int minValue(GameBoardState gameState, int alpha, int beta) {
        nodesExamined++; // håller reda på antal noder som sökts igenom
        int currentDepth = (int)gameState.getGameBoard().getCount(BoardCellState.ANY) - startCount; // antal brickor som ligger på brädet när metoden kallas

        if(currentDepth > searchDepth) searchDepth = currentDepth; // om sökdjupet har ökats så ökas currentdepth

        if(AgentController.isTerminal(gameState, playerTurn)) { // om terminal nod har hittats
            reachedLeafNodes++;
            return getUtility(gameState);
        }

        // kollar så att man inte sökt för djupt eller för länge. Antingen 5 sek, eller sökdjupet är större än djupgränsen och 35% av tiden passerat
        if(AgentController.cutOffTest(UserSettings.MAX_SEARCH_TIME,searchDepth,depthLimit,startTime)) {
            return getUtility(gameState);
        }

        int v = Integer.MAX_VALUE; // används för att hitta lägst utility
        List<ObjectiveWrapper> availableMoves = AgentController.getAvailableMoves(gameState, playerTurn);

        // går igenom alla möjliga drag och anropar maxValue
        for(int i = 0; i < availableMoves.size(); i++) {
            GameBoardState gbs = AgentController.getNewState(gameState, availableMoves.get(i));  // kopierar state för hur nästa move skulle se ut om vi gör det draget
            int utility = maxValue(gbs, alpha, beta); // anropa maxValue metoden för nästa game state

            if(utility < v) v = utility;

            if(v <= alpha) { // om v mindre än alpha
                prunedCounter += availableMoves.size() - 1 - i;
                return v; // prunar resterande noder
            }

            if(v < beta) beta = v; // om bättre beta värde hittas byts värdet ut
        }
        return v; // returnerar utility värdet
    }

    public int getUtility(GameBoardState gameState) {
        int utility;
        if(playerTurn == PlayerTurn.PLAYER_ONE) { // om ABplayer är vit
            utility = gameState.getWhiteCount() - gameState.getBlackCount(); // utility = vita - svarta brickor som ligger i det gamestate som vi skickar in
        }
        else { // om ABPlayer är svart
            utility = gameState.getBlackCount() - gameState.getWhiteCount(); // utility = vita - svarta brickor som ligger i det gamestate som vi skickar in
        }

        return utility;
    }
}
