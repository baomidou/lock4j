package com.baomidou.lock.executor;

import io.etcd.jetcd.*;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lock.LockResponse;
import io.etcd.jetcd.lock.UnlockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EtcdLockExecutorTest {

    @Mock
    private Client etcdClient;
    
    @Mock
    private Lock lockClient;
    
    @Mock
    private Lease leaseClient;
    
    @Mock
    private LeaseGrantResponse leaseGrantResponse;
    
    @Mock
    private LockResponse lockResponse;
    
    @Mock
    private UnlockResponse unlockResponse;

    private EtcdLockExecutor lockExecutor;

    @BeforeEach
    void setUp() {
        lockExecutor = new EtcdLockExecutor(etcdClient);
    }

    @Test
    void testRenewal() {
        assertTrue(lockExecutor.renewal(), "Renewal should return true");
    }

    @Test
    void testAcquireLock() throws Exception {
        String lockKey = "test-lock";
        String lockValue = "test-value";
        long expire = 30;
        long acquireTimeout = 3000;

        // Mock lease grant response
        when(leaseGrantResponse.getID()).thenReturn(123L);
        when(leaseClient.grant(anyLong())).thenReturn(CompletableFuture.completedFuture(leaseGrantResponse));
        when(etcdClient.getLeaseClient()).thenReturn(leaseClient);
        
        // Mock lock response
        when(lockClient.lock(any(ByteSequence.class), anyLong())).thenReturn(CompletableFuture.completedFuture(lockResponse));
        when(etcdClient.getLockClient()).thenReturn(lockClient);

        boolean result = lockExecutor.acquire(lockKey, lockValue, expire, acquireTimeout);
        assertTrue(result, "Lock acquisition should succeed with mocked responses");
    }

    @Test
    void testReleaseLock() throws Exception {
        String lockKey = "test-lock";
        String lockValue = "test-value";
        Boolean lockInstance = true;

        // Mock unlock response
        when(lockClient.unlock(any(ByteSequence.class))).thenReturn(CompletableFuture.completedFuture(unlockResponse));
        when(etcdClient.getLockClient()).thenReturn(lockClient);

        boolean result = lockExecutor.releaseLock(lockKey, lockValue, lockInstance);
        assertTrue(result, "Lock release should return true");
    }
}