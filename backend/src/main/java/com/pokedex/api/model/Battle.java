package com.pokedex.api.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Documento da coleção {@code battles}: estado completo de uma batalha PvP e o registro de cada
 * rodada. Fica salvo depois de terminar, alimentando o histórico e o ranking.
 */
@Document(collection = "battles")
@JsonPropertyOrder({"id", "status", "round", "winner", "finishReason", "player1", "player2", "log", "createdAt", "finishedAt"})
public class Battle {

    public enum Status { IN_PROGRESS, FINISHED }

    public enum FinishReason { KNOCKOUT, FORFEIT }

    @Id
    private String id;
    private Status status = Status.IN_PROGRESS;
    private int round;
    private Integer winner;
    private FinishReason finishReason;
    private BattleFighter player1;
    private BattleFighter player2;
    private List<BattleEvent> log = new ArrayList<>();
    private Instant createdAt;
    private Instant finishedAt;

    public BattleFighter fighter(int player) {
        return player == 1 ? player1 : player2;
    }

    public BattleFighter opponentOf(int player) {
        return player == 1 ? player2 : player1;
    }

    public boolean isFinished() {
        return status == Status.FINISHED;
    }

    public void finish(int winnerPlayer, FinishReason reason) {
        this.status = Status.FINISHED;
        this.winner = winnerPlayer;
        this.finishReason = reason;
        this.finishedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public Integer getWinner() {
        return winner;
    }

    public void setWinner(Integer winner) {
        this.winner = winner;
    }

    public FinishReason getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(FinishReason finishReason) {
        this.finishReason = finishReason;
    }

    public BattleFighter getPlayer1() {
        return player1;
    }

    public void setPlayer1(BattleFighter player1) {
        this.player1 = player1;
    }

    public BattleFighter getPlayer2() {
        return player2;
    }

    public void setPlayer2(BattleFighter player2) {
        this.player2 = player2;
    }

    public List<BattleEvent> getLog() {
        return log;
    }

    public void setLog(List<BattleEvent> log) {
        this.log = log == null ? new ArrayList<>() : new ArrayList<>(log);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
