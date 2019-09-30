package io.bdrc.ldspdi.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class TestResource {

    public final static Logger log = LoggerFactory.getLogger(TestResource.class.getName());
    public final String PUBLIC_GROUP = "public";
    public final String ADMIN_GROUP = "admin";
    public final String STAFF_GROUP = "staff";
    public final String READ_PUBLIC_ROLE = "readpublic";
    public final String READ_ONLY_PERM = "readonly";
    public final String READ_PRIVATE_PERM = "readprivate";

    @GetMapping("auth/public")
    public ResponseEntity<String> authPublicGroupTest() {
        System.out.println("Call to auth/public >>>>>>>>>");
        return ResponseEntity.status(200).header("Content-Type", "text/html").body("test auth public done");
    }

    @GetMapping("auth/rdf/admin")
    public ResponseEntity<String> authPrivateResourceAccessTest() {
        System.out.println("auth/ref/admin >>>>>>>>> ");
        return ResponseEntity.status(200).header("Content-Type", "text/html").body("test auth public done");
    }

}
