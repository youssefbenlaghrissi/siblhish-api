package ma.siblhish.controller;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.ApiResponse;
import ma.siblhish.dto.CardDto;
import ma.siblhish.service.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller pour gérer les cartes statistiques
 */
@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    /**
     * Obtenir toutes les cartes disponibles
     * @return Liste de toutes les cartes avec leurs informations
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CardDto>>> getAllCards() {
        List<CardDto> cards = cardService.getAllCards();
        return ResponseEntity.ok(ApiResponse.success(cards));
    }
}

