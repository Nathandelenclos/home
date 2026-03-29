package fr.nathandelenclos.home.application;

import fr.nathandelenclos.home.application.port.TpaRequestRepository;
import fr.nathandelenclos.home.domain.TpaRequest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TpaServiceTest {

    private static final UUID REQUESTER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TARGET = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void createRequestRejectsSelfRequest() {
        TpaService service = new TpaService(new InMemoryTpaRequestRepository(), 60_000L);

        TpaService.RequestStatus status = service.createRequest(REQUESTER, REQUESTER, 1_000L);

        assertEquals(TpaService.RequestStatus.SELF_REQUEST, status);
    }

    @Test
    void acceptRequestProcessesValidPendingRequest() {
        TpaService service = new TpaService(new InMemoryTpaRequestRepository(), 60_000L);
        service.createRequest(REQUESTER, TARGET, 1_000L);

        TpaService.DecisionResult result = service.acceptRequest(TARGET, 1_500L);

        assertEquals(TpaService.DecisionStatus.PROCESSED, result.status());
        assertEquals(REQUESTER, result.requesterId());
    }

    @Test
    void acceptRequestReturnsExpiredWhenRequestIsOld() {
        TpaService service = new TpaService(new InMemoryTpaRequestRepository(), 1_000L);
        service.createRequest(REQUESTER, TARGET, 1_000L);

        TpaService.DecisionResult result = service.acceptRequest(TARGET, 2_500L);

        assertEquals(TpaService.DecisionStatus.EXPIRED, result.status());
        assertEquals(REQUESTER, result.requesterId());
    }

    @Test
    void denyRequestReturnsNoPendingWhenEmpty() {
        TpaService service = new TpaService(new InMemoryTpaRequestRepository(), 60_000L);

        TpaService.DecisionResult result = service.denyRequest(TARGET, 1_000L);

        assertEquals(TpaService.DecisionStatus.NO_PENDING, result.status());
    }

    private static final class InMemoryTpaRequestRepository implements TpaRequestRepository {

        private final Map<UUID, TpaRequest> requests = new HashMap<>();

        @Override
        public void saveForTarget(UUID targetId, TpaRequest request) {
            requests.put(targetId, request);
        }

        @Override
        public Optional<TpaRequest> removeForTarget(UUID targetId) {
            return Optional.ofNullable(requests.remove(targetId));
        }
    }
}
