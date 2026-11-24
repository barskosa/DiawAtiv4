package com.pokedex.api.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Documento da coleção {@code battles}: um desafio do jogador contra um treinador. O jogador recebe
 * um time sorteado do mesmo tamanho da equipe do treinador. Guarda os dois times, os Pokémon ativos,
 * o vencedor e o registro de cada rodada; fica salvo para o histórico e as estatísticas.
 */
@Document(collection = "battles")
@JsonPropertyOrder({"id", "status", "round", "winner", "finishReason", "playerName", "dailyDate", "trainer",
        "playerActive", "trainerActive", "playerTeam", "trainerTeam", "log", "createdAt", "finishedAt"})
public class Battle {

    public enum Status { IN_PROGRESS, FINISHED }

    public enum FinishReason { KNOCKOUT, FORFEIT }

    /** Dados do treinador desafiado (cópia do documento da coleção trainers). */
    public record TrainerInfo(Integer id, String name, int badges) {
    }

    @Id
    private String id;
    private Status status = Status.IN_PROGRESS;
    private int round;
    private BattleSide winner;
    private FinishReason finishReason;
    private String playerName;
    /** Data do desafio do dia (ex.: 2026-11-24); {@code null} nos desafios livres. */
    private String dailyDate;
    private TrainerInfo trainer;
    private int playerActive;
    private int trainerActive;
    private List<BattleFighter> playerTeam = new ArrayList<>();
    private List<BattleFighter> trainerTeam = new ArrayList<>();
    private List<BattleEvent> log = new ArrayList<>();
    private Instant createdAt;
    private Instant finishedAt;

    public List<BattleFighter> team(BattleSide side) {
        return side == BattleSide.PLAYER ? playerTeam : trainerTeam;
    }

    public BattleFighter active(BattleSide side) {
        return team(side).get(side == BattleSide.PLAYER ? playerActive : trainerActive);
    }

    public void setActive(BattleSide side, int index) {
        if (side == BattleSide.PLAYER) {
            playerActive = index;
        } else {
            trainerActive = index;
        }
    }

    /** Nome de quem comanda o lado: o jogador ou o treinador. */
    public String sideName(BattleSide side) {
        return side == BattleSide.PLAYER ? playerName : trainer.name();
    }

    /** Pokémon que ainda não desmaiaram. */
    public long remaining(BattleSide side) {
        return team(side).stream().filter(fighter -> !fighter.isFainted()).count();
    }

    @JsonIgnore
    public boolean isFinished() {
        return status == Status.FINISHED;
    }

    public void finish(BattleSide winnerSide, FinishReason reason) {
        this.status = Status.FINISHED;
        this.winner = winnerSide;
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

    public BattleSide getWinner() {
        return winner;
    }

    public void setWinner(BattleSide winner) {
        this.winner = winner;
    }

    public FinishReason getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(FinishReason finishReason) {
        this.finishReason = finishReason;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getDailyDate() {
        return dailyDate;
    }

    public void setDailyDate(String dailyDate) {
        this.dailyDate = dailyDate;
    }

    /** HP somado do time do jogador, em porcentagem do total (critério de desempate do ranking do dia). */
    public double playerHpPercent() {
        int max = playerTeam.stream().mapToInt(BattleFighter::getMaxHp).sum();
        int current = playerTeam.stream().mapToInt(BattleFighter::getCurrentHp).sum();
        return max == 0 ? 0 : Math.round(current * 1000.0 / max) / 10.0;
    }

    public TrainerInfo getTrainer() {
        return trainer;
    }

    public void setTrainer(TrainerInfo trainer) {
        this.trainer = trainer;
    }

    public int getPlayerActive() {
        return playerActive;
    }

    public void setPlayerActive(int playerActive) {
        this.playerActive = playerActive;
    }

    public int getTrainerActive() {
        return trainerActive;
    }

    public void setTrainerActive(int trainerActive) {
        this.trainerActive = trainerActive;
    }

    public List<BattleFighter> getPlayerTeam() {
        return playerTeam;
    }

    public void setPlayerTeam(List<BattleFighter> playerTeam) {
        this.playerTeam = playerTeam == null ? new ArrayList<>() : new ArrayList<>(playerTeam);
    }

    public List<BattleFighter> getTrainerTeam() {
        return trainerTeam;
    }

    public void setTrainerTeam(List<BattleFighter> trainerTeam) {
        this.trainerTeam = trainerTeam == null ? new ArrayList<>() : new ArrayList<>(trainerTeam);
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
