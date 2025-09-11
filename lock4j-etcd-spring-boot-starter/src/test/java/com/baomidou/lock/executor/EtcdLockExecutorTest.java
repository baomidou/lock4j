package com.baomidou.lock.executor;

import io.etcd.jetcd.*;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lock.LockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

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
    
    private ByteSequence mockLockKey;

    private EtcdLockExecutor lockExecutor;

    @BeforeEach
    void setUp() {
        lockExecutor = new EtcdLockExecutor(etcdClient);
        // 创建真实的ByteSequence对象而不是mock
        mockLockKey = ByteSequence.from("test-lock-key".getBytes());
    }

    @Test
    void testRenewal() {
        // Etcd不支持自动续期
        assertFalse(lockExecutor.renewal(), "Etcd lock executor should not support renewal");
    }

    @Test
    void testAcquireLockSuccess() throws Exception {
        String lockKey = "test-lock";
        String lockValue = "test-value";
        long expire = 30000; // 30秒
        long acquireTimeout = 3000; // 3秒

        // Mock lease相关操作
        when(leaseGrantResponse.getID()).thenReturn(123L);
        when(leaseClient.grant(anyLong())).thenReturn(CompletableFuture.completedFuture(leaseGrantResponse));
        when(etcdClient.getLeaseClient()).thenReturn(leaseClient);
        
        // Mock lock相关操作
        when(lockResponse.getKey()).thenReturn(mockLockKey);
        when(lockClient.lock(any(ByteSequence.class), anyLong())).thenReturn(CompletableFuture.completedFuture(lockResponse));
        when(etcdClient.getLockClient()).thenReturn(lockClient);

        EtcdLockInfo result = lockExecutor.acquire(lockKey, lockValue, expire, acquireTimeout);
        
        assertNotNull(result, "Lock acquisition should succeed");
        assertEquals(123L, result.getLeaseId(), "Lease ID should match");
        assertEquals(lockValue, result.getLockValue(), "Lock value should match");
        assertNotNull(result.getLockKey(), "Lock key should not be null");
        
        // 验证调用次数
        verify(leaseClient, times(1)).grant(30L); // expire/1000
        verify(lockClient, times(1)).lock(any(ByteSequence.class), eq(123L));
    }

    @Test
    void testAcquireLockFailure() throws Exception {
        String lockKey = "test-lock";
        String lockValue = "test-value";
        long expire = 30000;
        long acquireTimeout = 3000;

        // Mock lease相关操作
        when(leaseGrantResponse.getID()).thenReturn(123L);
        when(leaseClient.grant(anyLong())).thenReturn(CompletableFuture.completedFuture(leaseGrantResponse));
        when(leaseClient.revoke(anyLong())).thenReturn(CompletableFuture.completedFuture(mock(io.etcd.jetcd.lease.LeaseRevokeResponse.class)));
        when(etcdClient.getLeaseClient()).thenReturn(leaseClient);
        
        // Mock lock操作返回null（获取锁失败）
        when(lockClient.lock(any(ByteSequence.class), anyLong())).thenReturn(CompletableFuture.completedFuture(null));
        when(etcdClient.getLockClient()).thenReturn(lockClient);

        EtcdLockInfo result = lockExecutor.acquire(lockKey, lockValue, expire, acquireTimeout);
        
        assertNull(result, "Lock acquisition should fail");
        
        // 验证租约被撤销
        verify(leaseClient, times(1)).revoke(123L);
    }

    @Test
    void testAcquireLockTimeout() throws Exception {
        String lockKey = "test-lock";
        String lockValue = "test-value";
        long expire = 30000;
        long acquireTimeout = 100; // 很短的超时时间

        // Mock lease操作超时
        CompletableFuture<LeaseGrantResponse> timeoutFuture = new CompletableFuture<>();
        when(leaseClient.grant(anyLong())).thenReturn(timeoutFuture);
        when(etcdClient.getLeaseClient()).thenReturn(leaseClient);

        EtcdLockInfo result = lockExecutor.acquire(lockKey, lockValue, expire, acquireTimeout);
        
        assertNull(result, "Lock acquisition should timeout and return null");
    }

    @Test
    void testAcquireLockWithNullKey() {
        EtcdLockInfo result = lockExecutor.acquire(null, "test-value", 30000, 3000);
        assertNull(result, "Should return null for null lock key");
        
        result = lockExecutor.acquire("", "test-value", 30000, 3000);
        assertNull(result, "Should return null for empty lock key");
        
        result = lockExecutor.acquire("   ", "test-value", 30000, 3000);
        assertNull(result, "Should return null for blank lock key");
    }

    @Test
    void testReleaseLockSuccess() throws Exception {
        String lockKey = "test-lock";
        String lockValue = "test-value";
        EtcdLockInfo lockInfo = new EtcdLockInfo(mockLockKey, 123L, lockValue);

        // Mock unlock操作
        when(lockClient.unlock(any(ByteSequence.class))).thenReturn(CompletableFuture.completedFuture(mock(io.etcd.jetcd.lock.UnlockResponse.class)));
        when(etcdClient.getLockClient()).thenReturn(lockClient);
        
        // Mock lease revoke操作
        when(leaseClient.revoke(anyLong())).thenReturn(CompletableFuture.completedFuture(mock(io.etcd.jetcd.lease.LeaseRevokeResponse.class)));
        when(etcdClient.getLeaseClient()).thenReturn(leaseClient);

        boolean result = lockExecutor.releaseLock(lockKey, lockValue, lockInfo);
        
        assertTrue(result, "Lock release should succeed");
        verify(lockClient, times(1)).unlock(mockLockKey);
        verify(leaseClient, times(1)).revoke(123L);
    }

    @Test
    void testReleaseLockWithNullLockInfo() {
        boolean result = lockExecutor.releaseLock("test-key", "test-value", null);
        assertFalse(result, "Should return false for null lockInfo");
    }

    @Test
    void testReleaseLockWithValueMismatch() {
        String lockKey = "test-lock";
        String correctValue = "correct-value";
        String wrongValue = "wrong-value";
        EtcdLockInfo lockInfo = new EtcdLockInfo(mockLockKey, 123L, correctValue);

        boolean result = lockExecutor.releaseLock(lockKey, wrongValue, lockInfo);
        
        assertFalse(result, "Should return false for value mismatch");
        // 验证没有进行unlock和revoke操作
        verifyNoInteractions(lockClient);
        verifyNoInteractions(leaseClient);
    }

    @Test
    void testReleaseLockPartialFailure() throws Exception {
        String lockKey = "test-lock";
        String lockValue = "test-value";
        EtcdLockInfo lockInfo = new EtcdLockInfo(mockLockKey, 123L, lockValue);

        // Mock unlock成功，但lease revoke失败
        when(lockClient.unlock(any(ByteSequence.class))).thenReturn(CompletableFuture.completedFuture(mock(io.etcd.jetcd.lock.UnlockResponse.class)));
        when(etcdClient.getLockClient()).thenReturn(lockClient);
        
        CompletableFuture<io.etcd.jetcd.lease.LeaseRevokeResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Revoke failed"));
        when(leaseClient.revoke(anyLong())).thenReturn(failedFuture);
        when(etcdClient.getLeaseClient()).thenReturn(leaseClient);

        boolean result = lockExecutor.releaseLock(lockKey, lockValue, lockInfo);
        
        assertFalse(result, "Lock release should fail due to lease revoke failure");
        verify(lockClient, times(1)).unlock(mockLockKey);
        verify(leaseClient, times(1)).revoke(123L);
    }
}