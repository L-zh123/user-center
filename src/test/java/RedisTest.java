
import java.util.Date;import com.lzh.usercenter.UserCenterApplication;
import com.lzh.usercenter.pojo.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import javax.annotation.Resource;


@SpringBootTest(classes = UserCenterApplication.class)
public class RedisTest {

    @Resource
    private RedisTemplate redisTemplate;

    @Test
    public void test(){
        redisTemplate.opsForValue().set("nameStr","1223");
        redisTemplate.opsForValue().set("nameInt",123);
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        redisTemplate.opsForValue().set("nameUser",user);
        //查
        Object nameStr = redisTemplate.opsForValue().get("nameStr");
        System.out.println("nameStr = " + nameStr);
        Object nameInt = redisTemplate.opsForValue().get("nameInt");
        System.out.println("nameInt = " + nameInt);
        Object nameUser = redisTemplate.opsForValue().get("nameUser");
        System.out.println("nameUser = " + nameUser);
    }
}
