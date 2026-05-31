package com.example.ecommerce.cart.service;

import com.example.ecommerce.cart.dto.AddToCartRequest;
import com.example.ecommerce.cart.dto.CartResponse;
import com.example.ecommerce.cart.dto.UpdateCartItemRequest;
import com.example.ecommerce.cart.entity.Cart;
import com.example.ecommerce.cart.entity.CartItem;
import com.example.ecommerce.cart.repository.CartItemRepository;
import com.example.ecommerce.cart.repository.CartRepository;
import com.example.ecommerce.common.exception.ApiException;
import com.example.ecommerce.product.entity.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.user.entity.User;
import com.example.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public CartResponse getCart(String email) {
        User user = findUser(email);
        Cart cart = findOrCreateCart(user);
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse addItem(String email, AddToCartRequest request) {
        User user = findUser(email);
        Cart cart = findOrCreateCart(user);
        Product product = findActiveProduct(request.getProductId());

        if (request.getQuantity() > product.getStockQuantity()) {
            throw new ApiException("Requested quantity exceeds available stock", HttpStatus.BAD_REQUEST);
        }

        cartItemRepository.findByCartAndProduct(cart, product).ifPresentOrElse(
                existing -> existing.setQuantity(existing.getQuantity() + request.getQuantity()),
                () -> cart.getItems().add(CartItem.builder()
                        .cart(cart)
                        .product(product)
                        .quantity(request.getQuantity())
                        .unitPrice(product.getPrice())
                        .build())
        );

        return CartResponse.from(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse updateItem(String email, Long itemId, UpdateCartItemRequest request) {
        Cart cart = findCartByEmail(email);
        CartItem item = findItemInCart(cart, itemId);

        if (request.getQuantity() > item.getProduct().getStockQuantity()) {
            throw new ApiException("Requested quantity exceeds available stock", HttpStatus.BAD_REQUEST);
        }

        item.setQuantity(request.getQuantity());
        return CartResponse.from(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse removeItem(String email, Long itemId) {
        Cart cart = findCartByEmail(email);
        CartItem item = findItemInCart(cart, itemId);
        cart.getItems().remove(item);
        return CartResponse.from(cartRepository.save(cart));
    }

    @Transactional
    public void clearCart(String email) {
        Cart cart = findCartByEmail(email);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private Cart findOrCreateCart(User user) {
        return cartRepository.findByUser(user).orElseGet(() ->
                cartRepository.save(Cart.builder().user(user).build()));
    }

    private Cart findCartByEmail(String email) {
        User user = findUser(email);
        return cartRepository.findByUser(user)
                .orElseThrow(() -> new ApiException("Cart not found", HttpStatus.NOT_FOUND));
    }

    private CartItem findItemInCart(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ApiException("Cart item not found", HttpStatus.NOT_FOUND));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    private Product findActiveProduct(Long productId) {
        return productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> new ApiException("Product not found", HttpStatus.NOT_FOUND));
    }
}