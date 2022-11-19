import com.lzh.usercenter.UserCenterApplication;
import com.lzh.usercenter.pojo.vo.SafetyUser;
import com.lzh.usercenter.service.UserService;
import com.lzh.usercenter.utils.R;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@SpringBootTest(classes = UserCenterApplication.class)
public class TestApplication {
    @Resource
    private UserService userService;

    @Test
    public void test1(){

        List<String> tagNameList = Arrays.asList("java","python");
        R<List<SafetyUser>> list = userService.searchUsersByTags(tagNameList);
        System.out.println("list = " + list);
    }

}
