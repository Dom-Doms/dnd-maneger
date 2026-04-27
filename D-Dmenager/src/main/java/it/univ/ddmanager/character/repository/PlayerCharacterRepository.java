package it.univ.ddmanager.character.repository;

import it.univ.ddmanager.character.model.PlayerCharacter;
import it.univ.ddmanager.user.model.User;
import it.univ.ddmanager.world.model.World;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerCharacterRepository extends JpaRepository<PlayerCharacter, Long> {

    List<PlayerCharacter> findByOwner(User owner);

    List<PlayerCharacter> findByWorld(World world);

    List<PlayerCharacter> findByWorldId(Long worldId);

    List<PlayerCharacter> findByWorldIdAndOwnerId(Long worldId, Long ownerId);

    List<PlayerCharacter> findByWorldIdAndIsVisibleToPlayersTrue(Long worldId);
}
