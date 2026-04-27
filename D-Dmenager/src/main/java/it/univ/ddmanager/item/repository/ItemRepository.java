// src/main/java/it/univ/ddmanager/item/repository/ItemRepository.java
package it.univ.ddmanager.item.repository;

import it.univ.ddmanager.item.model.Item;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByWorldId(Long worldId);

    List<Item> findByWorldIdAndIsVisibleToPlayersTrue(Long worldId);

    List<Item> findByIsVisibleToPlayersTrue();
}
