package aspect.withaspect;

import aspect.core.IdempotentTestPayload;
import aspect.core.TestIdempotentResource;
import com.trendyol.jdempotent.core.annotation.JdempotentResource;
import com.trendyol.jdempotent.core.aspect.IdempotentAspect;
import com.trendyol.jdempotent.core.callback.ErrorConditionalCallback;
import com.trendyol.jdempotent.core.datasource.IdempotentRepository;
import com.trendyol.jdempotent.core.generator.DefaultKeyGenerator;
import com.trendyol.jdempotent.core.model.IdempotentIgnorableWrapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestIdempotentResource.class})
public class IdempotentAspectUT {

    @InjectMocks
    private IdempotentAspect idempotentAspect;

    @Mock
    private IdempotentRepository idempotentRepository;

    @Mock
    private DefaultKeyGenerator defaultKeyGenerator;

    @Mock
    private ErrorConditionalCallback errorCallback;

    @Test
    public void given_new_payload_when_key_not_in_repository_and_method_has_one_arg_then_should_store_repository() throws Throwable {
        //given
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = TestIdempotentResource.class.getMethod("idempotentMethod", IdempotentTestPayload.class);

        IdempotentTestPayload payload = new IdempotentTestPayload("payload");
        TestIdempotentResource testIdempotentResource = mock(TestIdempotentResource.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{payload});
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(testIdempotentResource);
        when(joinPoint.getTarget().getClass().getSimpleName()).thenReturn("TestIdempotentResource");
        when(idempotentRepository.contains(any())).thenReturn(false);

        //when
        idempotentAspect.execute(joinPoint);

        //then
        verify(joinPoint, times(4)).getSignature();
        verify(signature, times(3)).getMethod();
        verify(joinPoint).getTarget();
        verify(idempotentRepository, times(1)).store(any(), any(), any(), any());
        verify(joinPoint).proceed();
        verify(idempotentRepository, times(1)).setResponse(any(), any(), any(), any(), any());
    }

    @Test
    public void given_actual_payload_when_key_in_repository_and_method_has_one_arg_then_should_not_store_repository() throws Throwable {
        //given
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = TestIdempotentResource.class.getMethod("idempotentMethod", IdempotentTestPayload.class);
        JdempotentResource jdempotentResource = mock(JdempotentResource.class);
        IdempotentTestPayload payload = new IdempotentTestPayload("payload");
        TestIdempotentResource testIdempotentResource = mock(TestIdempotentResource.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{payload});
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(testIdempotentResource);
        when(joinPoint.getTarget().getClass().getSimpleName()).thenReturn("TestIdempotentResource");
        when(idempotentRepository.contains(any())).thenReturn(true);

        //when
        idempotentAspect.execute(joinPoint);

        //then
        verify(joinPoint, times(4)).getSignature();
        verify(signature, times(3)).getMethod();
        verify(joinPoint).getTarget();
        verify(idempotentRepository, times(0)).store(any(), any());
        verify(joinPoint, times(0)).proceed();
        verify(idempotentRepository, times(0)).setResponse(any(), any(), any());
        verify(idempotentRepository, times(1)).getResponse(any());
    }

    @Test(expected = RuntimeException.class)
    public void given_actual_payload_when_key_in_repository_and_method_has_one_arg_then_should_store_repository_before_should_be_delete() throws Throwable {
        //given
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = TestIdempotentResource.class.getMethod("idempotentMethodThrowingARuntimeException", IdempotentTestPayload.class);
        JdempotentResource jdempotentResource = mock(JdempotentResource.class);
        IdempotentTestPayload payload = new IdempotentTestPayload("payload");
        TestIdempotentResource testIdempotentResource = mock(TestIdempotentResource.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{payload});
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(testIdempotentResource);
        when(joinPoint.proceed()).thenThrow(new RuntimeException());
        when(joinPoint.getTarget().getClass().getSimpleName()).thenReturn("TestIdempotentResource");
        when(idempotentRepository.contains(any())).thenReturn(false);

        //when
        idempotentAspect.execute(joinPoint);

        //then
        verify(joinPoint, times(2)).getSignature();
        verify(signature).getMethod();
        verify(joinPoint).getTarget();
        verify(idempotentRepository, times(1)).store(any(), any());
        verify(joinPoint, times(1)).proceed();
        verify(idempotentRepository, times(1)).remove(any());
        verify(idempotentRepository, times(0)).setResponse(any(), any(), any());
        verify(idempotentRepository, times(0)).getResponse(any());
    }

    @Test(expected = IllegalStateException.class)
    public void not_given_a_payload_then_return_exception() throws Throwable {
        //given
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = TestIdempotentResource.class.getMethod("idempotentMethodWithZeroParamater");
        JdempotentResource jdempotentResource = mock(JdempotentResource.class);
        IdempotentTestPayload payload = new IdempotentTestPayload("payload");
        TestIdempotentResource testIdempotentResource = mock(TestIdempotentResource.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(testIdempotentResource);
        when(joinPoint.getTarget().getClass().getSimpleName()).thenReturn("TestIdempotentResource");
        when(idempotentRepository.contains(any())).thenReturn(false);

        //when
        idempotentAspect.execute(joinPoint);

        //then
        verify(joinPoint).getTarget();
        verify(joinPoint).getSignature();
        verify(signature, times(0)).getMethod();
        verify(idempotentRepository, times(0)).store(any(), any());
        verify(joinPoint, times(0)).proceed();
        verify(idempotentRepository, times(0)).setResponse(any(), any(), any());
    }

    @Test(expected = RuntimeException.class)
    public void given_a_payload_when_called_error_callback_then_should_return_exception() throws Throwable {
        //given
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = TestIdempotentResource.class.getMethod("idempotentMethodThrowingARuntimeException", IdempotentTestPayload.class);
        IdempotentTestPayload payload = new IdempotentTestPayload("payload");
        TestIdempotentResource testIdempotentResource = mock(TestIdempotentResource.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{payload});
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(testIdempotentResource);
        when(joinPoint.proceed()).thenThrow(new RuntimeException());
        when(joinPoint.getTarget().getClass().getSimpleName()).thenReturn("TestIdempotentResource");
        when(idempotentRepository.contains(any())).thenReturn(false);
        when(errorCallback.onErrorCondition(any())).thenReturn(true);
        when(errorCallback.onErrorCustomException()).thenReturn(new RuntimeException());

        //when
        idempotentAspect.execute(joinPoint);

        //then
        verify(joinPoint, times(2)).getSignature();
        verify(signature).getMethod();
        verify(joinPoint).getTarget();
        verify(idempotentRepository, times(1)).store(any(), any());
        verify(joinPoint).proceed();
        verify(idempotentRepository, times(0)).setResponse(any(), any(), any());
        verify(idempotentRepository, times(1)).remove(any());
    }

    @Test
    public void given_a_payload_include_one_parameter_when_find_idempotent_request_then_return_idempotent_ignorable_wrapper() throws Throwable {
        //given
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = TestIdempotentResource.class.getMethod("idempotentMethod", IdempotentTestPayload.class);

        IdempotentTestPayload payload = new IdempotentTestPayload("payload");
        TestIdempotentResource testIdempotentResource = mock(TestIdempotentResource.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{payload});
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(testIdempotentResource);
        when(joinPoint.getTarget().getClass().getSimpleName()).thenReturn("TestIdempotentResource");
        when(idempotentRepository.contains(any())).thenReturn(false);

        //when
        var idempotentRequestWrapper = idempotentAspect.findIdempotentRequestArg(joinPoint);

        //then
        IdempotentIgnorableWrapper requestWrapperRequest = (IdempotentIgnorableWrapper) idempotentRequestWrapper.getRequest();
        assertEquals(requestWrapperRequest.getNonIgnoredFields().size(), 1);
        assertEquals(requestWrapperRequest.getNonIgnoredFields().get("name"), "payload");
        verify(joinPoint).getArgs();
    }
}
