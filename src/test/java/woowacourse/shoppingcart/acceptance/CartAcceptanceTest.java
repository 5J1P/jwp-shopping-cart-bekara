package woowacourse.shoppingcart.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;
import static woowacourse.shoppingcart.acceptance.CustomerAcceptanceTest.createCustomer;
import static woowacourse.shoppingcart.acceptance.CustomerAcceptanceTest.getTokenResponse;
import static woowacourse.shoppingcart.acceptance.ProductAcceptanceTest.상품_등록되어_있음;
import static woowacourse.shoppingcart.fixture.CustomerFixtures.CUSTOMER_REQUEST_1;
import static woowacourse.shoppingcart.fixture.ProductFixtures.PRODUCT_1;
import static woowacourse.shoppingcart.fixture.ProductFixtures.PRODUCT_2;
import static woowacourse.shoppingcart.fixture.ProductFixtures.PRODUCT_REQUEST_1;
import static woowacourse.shoppingcart.fixture.ProductFixtures.PRODUCT_REQUEST_2;
import static woowacourse.shoppingcart.fixture.ProductFixtures.getProductRequestParam;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import woowacourse.AcceptanceTest;
import woowacourse.shoppingcart.dto.CartItemResponse;
import woowacourse.shoppingcart.dto.CartItemResponses;
import woowacourse.shoppingcart.dto.ProductResponse;

@DisplayName("장바구니 관련 기능")
public class CartAcceptanceTest extends AcceptanceTest {
    public static final String EXPIRED_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNjU0MzUyNzMwLCJleHAiOjE2NTQzNTI3MzB9.OvlNgJk_dG30BL_JWj_DQRPmepqLMLl6Djwtlp2hBWw";
    public static final String INVALID_TOKEN = "invalidToken";
    public static final int PRODUCT_QUANTITY_1 = 3;
    public static final int PRODUCT_QUANTITY_2 = 100;

    private Long productId1;
    private Long productId2;
    private String token;

    public static ExtractableResponse<Response> 장바구니_아이템_추가_요청(Long productId, int quantity, String token) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("productId", productId);
        requestBody.put("quantity", quantity);

        return RestAssured
                .given().log().all()
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(requestBody)
                .when().post("/api/customers/cart")
                .then().log().all()
                .extract();
    }

    public static Long 장바구니_아이템_추가되어_있음(Long productId, int quantity, String token) {
        ExtractableResponse<Response> response = 장바구니_아이템_추가_요청(productId, quantity, token);
        return Long.parseLong(response.header("Location").split("/cart/")[1]);
    }

    public static ExtractableResponse<Response> 장바구니_아이템_목록_조회_요청(String token) {
        return RestAssured
                .given().log().all()
                .header("Authorization", "Bearer" + token)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when().get("/api/customers/cart")
                .then().log().all()
                .extract();
    }


    public static ExtractableResponse<Response> 장바구니_삭제_요청(Long cartId, String token) {
        return RestAssured
                .given().log().all()
                .header("Authorization", "Bearer" + token)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when().delete("/api/customers/cart/{cartId}", cartId)
                .then().log().all()
                .extract();
    }

    public static void 장바구니_아이템_추가됨(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.header("Location")).isNotBlank();
    }

    private void 만료된_토큰으로_요청시_확인(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    private void 유효하지_않은_토큰으로_요청시_확인(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

    }
    private void 장바구니_아이템_추가_안됨_없는_물건(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());

    }

    public static void 장바구니_아이템_목록_응답됨(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    private void 장바구니_아이템_목록_확인(ExtractableResponse<Response> response) {
        final CartItemResponses cartItemResponses = response.jsonPath().getObject(".", CartItemResponses.class);

        final List<ProductResponse> productResponses = cartItemResponses.getCart()
                .stream()
                .map(CartItemResponse::getProductResponse).collect(
                        Collectors.toList());
        final List<Integer> quantities = cartItemResponses.getCart().stream().map(CartItemResponse::getQuantity)
                .collect(Collectors.toList());

        assertAll(
                () -> assertThat(productResponses).extracting("name", "price", "imageUrl", "description", "stock")
                        .containsExactly(
                                tuple(PRODUCT_1.getName(), PRODUCT_1.getPrice(),
                                        PRODUCT_1.getImageUrl(), PRODUCT_1.getDescription(),
                                        PRODUCT_1.getStock()),
                                tuple(PRODUCT_2.getName(), PRODUCT_2.getPrice(),
                                        PRODUCT_2.getImageUrl(), PRODUCT_2.getDescription(),
                                        PRODUCT_2.getStock())
                        ),
                () -> assertThat(quantities).hasSize(2).containsExactly(PRODUCT_QUANTITY_1, PRODUCT_QUANTITY_2)
        );
    }

    public static void 장바구니_삭제됨(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
    }

    private void 장바구니_삭제_안됨(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());

    }

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        createCustomer(CUSTOMER_REQUEST_1);
        token = getTokenResponse(CUSTOMER_REQUEST_1.getEmail(),
                CUSTOMER_REQUEST_1.getPassword()).getAccessToken();
        productId1 = 상품_등록되어_있음(getProductRequestParam(PRODUCT_REQUEST_1));
        productId2 = 상품_등록되어_있음(getProductRequestParam(PRODUCT_REQUEST_2));
    }

    @DisplayName("장바구니 아이템 추가")
    @Test
    void addCartItem() {
        ExtractableResponse<Response> response = 장바구니_아이템_추가_요청(productId1, PRODUCT_QUANTITY_1, token);

        장바구니_아이템_추가됨(response);
    }

    @DisplayName("만료된 토큰으로 장바구니 아이템 추가 시 403 Forbidden")
    @Test
    void addCartItemByExpiredToken() {
        ExtractableResponse<Response> response = 장바구니_아이템_추가_요청(productId1, PRODUCT_QUANTITY_1, EXPIRED_TOKEN);

        만료된_토큰으로_요청시_확인(response);
    }

    @DisplayName("유효하지 않은 토큰으로 장바구니 아이템 추가 시 401 Unauthorized")
    @Test
    void addCartItemByInvalidToken() {
        ExtractableResponse<Response> response = 장바구니_아이템_추가_요청(productId1, PRODUCT_QUANTITY_1, INVALID_TOKEN);

        유효하지_않은_토큰으로_요청시_확인(response);
    }

    @DisplayName("없는 물건을 장바구니 아이템으로추가 시 400 Bad request")
    @Test
    void addCartItemByNotExistProduct() {
        ExtractableResponse<Response> response = 장바구니_아이템_추가_요청(99999999L, PRODUCT_QUANTITY_1, token);

        장바구니_아이템_추가_안됨_없는_물건(response);
    }


    @DisplayName("장바구니 아이템 목록 조회")
    @Test
    void getCartItems() {
        장바구니_아이템_추가되어_있음(productId1, PRODUCT_QUANTITY_1, token);
        장바구니_아이템_추가되어_있음(productId2, PRODUCT_QUANTITY_2, token);

        ExtractableResponse<Response> response = 장바구니_아이템_목록_조회_요청(token);

        장바구니_아이템_목록_응답됨(response);
        장바구니_아이템_목록_확인(response);
    }

    @DisplayName("유효하지 않은 토큰으로 장바구니 아이템 목록 조회시 401 Unauthorized")
    @Test
    void getCartItemsByInvalidToken() {
        ExtractableResponse<Response> response = 장바구니_아이템_목록_조회_요청(INVALID_TOKEN);

        유효하지_않은_토큰으로_요청시_확인(response);
    }

    @DisplayName("만료된 토큰으로 장바구니 아이템 목록 조회시 403 Forbidden")
    @Test
    void getCartItemsByExpiredToken() {
        ExtractableResponse<Response> response = 장바구니_아이템_목록_조회_요청(EXPIRED_TOKEN);

        만료된_토큰으로_요청시_확인(response);
    }

    @DisplayName("장바구니 삭제")
    @Test
    void deleteCartItem() {
        Long cartId = 장바구니_아이템_추가되어_있음(productId1, PRODUCT_QUANTITY_1, token);

        ExtractableResponse<Response> response = 장바구니_삭제_요청(cartId, token);

        장바구니_삭제됨(response);
    }

    @DisplayName("장바구니 삭제 안됨")
    @Test
    void deleteCartItemByInvalidCartId() {
        Long cartId = 장바구니_아이템_추가되어_있음(productId1, PRODUCT_QUANTITY_1, token);

        ExtractableResponse<Response> response = 장바구니_삭제_요청(cartId + 1, token);

        장바구니_삭제_안됨(response);
    }
}
