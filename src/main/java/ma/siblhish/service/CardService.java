package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.CardDto;
import ma.siblhish.entities.Card;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.CardRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final EntityMapper mapper;

    /**
     * Obtenir toutes les cartes disponibles
     * @return Liste de toutes les cartes avec leurs informations
     */
    public List<CardDto> getAllCards() {
        List<Card> cards = cardRepository.findAllByOrderByIdAsc();
        return cards.stream()
                .map(this::mapToCardDto)
                .toList();
    }

    /**
     * Mapper une entité Card vers un CardDto
     */
    private CardDto mapToCardDto(Card card) {
        if (card == null) return null;
        return new CardDto(
                card.getId(),
                card.getCode(),
                card.getTitle()
        );
    }
}

