package com.pokedex.api.service.battle;

import java.util.random.RandomGenerator;

import org.springframework.stereotype.Component;

import com.pokedex.api.model.BattleAction;
import com.pokedex.api.model.BattleFighter;

/**
 * "Inteligência" simples dos treinadores: usa o golpe com maior dano esperado contra o Pokémon do
 * jogador (levando em conta a tabela de tipos) e, com pouco HP (menos de 25%), às vezes se defende.
 */
@Component
public class TrainerAi {

    private final RandomGenerator random;

    public TrainerAi(RandomGenerator random) {
        this.random = random;
    }

    public BattleAction choose(BattleFighter self, BattleFighter foe) {
        boolean lowHp = self.getCurrentHp() * 4 < self.getMaxHp();
        if (lowHp && self.getLastAction() != BattleAction.DEFEND && random.nextInt(3) == 0) {
            return BattleAction.DEFEND;
        }
        double attack = BattleEngine.expectedDamage(self, foe, BattleAction.ATTACK);
        double special = BattleEngine.expectedDamage(self, foe, BattleAction.SPECIAL);
        return special > attack ? BattleAction.SPECIAL : BattleAction.ATTACK;
    }
}
