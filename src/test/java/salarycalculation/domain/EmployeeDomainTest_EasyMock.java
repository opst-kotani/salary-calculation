package salarycalculation.domain;

import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * {@link EmployeeDomain}に対する EasyMock を使ったテストクラス。
 *
 * @author naotake
 */
public class EmployeeDomainTest_EasyMock {

    private EmployeeDomain testee;

    @Test
    public void 入社年数を取得できること() {
        // パーシャルモックを用意
        testee = createMockBuilder(EmployeeDomain.class).addMockedMethod("getDurationMonth")
                .createMock();

        // 振る舞いを定義
        expect(testee.getDurationMonth()).andReturn(12).andReturn(24).andReturn(36);

        // 再生
        replay(testee);

        // 実行
        assertThat(testee.getDurationYear(), is(1)); // Month: 12
        assertThat(testee.getDurationYear(), is(2)); // Month: 24
        assertThat(testee.getDurationYear(), is(3)); // Month: 36

        // 検証
        verify(testee);
    }
}
