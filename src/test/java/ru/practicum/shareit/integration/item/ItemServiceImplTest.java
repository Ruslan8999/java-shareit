package ru.practicum.shareit.integration.item;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.exceptions.ObjectNotFoundException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;


@RequiredArgsConstructor(onConstructor_ = @Autowired)
@SpringBootTest(properties = "db.name=test1", webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ItemServiceImplTest {
    private final ItemService itemService;
    private final UserService userService;
    private final BookingService bookingService;

    @Order(1)
    @Test
    void testSaveItem() {
        UserDto userDto = UserDto.builder().id(1L).email("user1@mail.ru").name("User1").build();
        userService.createUser(userDto);
        ItemDto itemDto = ItemDto.builder().description("Item1desc").name("Item1").available(true).build();
        ItemDto item = itemService.addItem(1L, itemDto);
        assertThat(item.getId(), notNullValue());
        assertThat(item.getName(), equalTo(itemDto.getName()));
        assertThat(item.getDescription(), equalTo(itemDto.getDescription()));
        assertThat(item.getAvailable(), equalTo(itemDto.getAvailable()));
    }

    @Order(2)
    @Test
    void testUpdateItem() {
        ItemDto itemDto = ItemDto.builder().description("Item1descNew").name("Item1New").available(true).build();
        ItemDto item = itemService.editItem(1L, 1L, itemDto);
        assertThat(item.getId(), notNullValue());
        assertThat(item.getName(), equalTo(itemDto.getName()));
        assertThat(item.getDescription(), equalTo(itemDto.getDescription()));
        assertThat(item.getAvailable(), equalTo(itemDto.getAvailable()));
    }

    @Order(3)
    @Test
    void testGetItem() {
        ItemDto itemDto = ItemDto.builder().description("Item1descNew").name("Item1New").available(true).build();
        ItemDto item = itemService.getItem(1L, 1L);
        assertThat(item.getId(), notNullValue());
        assertThat(item.getName(), equalTo(itemDto.getName()));
        assertThat(item.getDescription(), equalTo(itemDto.getDescription()));
        assertThat(item.getAvailable(), equalTo(itemDto.getAvailable()));
    }

    @Order(7)
    @Test
    void testGetAllItem() {
        Collection<ItemDto> items = itemService.getAllItems(1L);
        assertThat(items.size(), notNullValue());
    }

    @Order(4)
    @Test
    void testSearch() {
        Collection<ItemDto> items = itemService.searchItemByText("Item1");
        assertThat(items.size(), equalTo(1));
    }

    @Order(5)
    @Test
    void testCreateComment() throws InterruptedException {
        UserDto userDto = UserDto.builder().email("user2@mail.ru").name("User2").build();
        UserDto user1 = userService.createUser(userDto);
        User user = new User();
        UserMapper.toUser(user1, user);
        LocalDateTime start = LocalDateTime.now().plusSeconds(1);
        LocalDateTime end = LocalDateTime.now().plusSeconds(2);
        CommentDto commentDto = CommentDto.builder().text("Comment1").created(LocalDateTime.now()).authorName("User1").build();
        BookingDto bookingDto = BookingDto.builder().itemId(1L).booker(user)
                .start(start)
                .end(end)
                .build();
        bookingService.createBooking(bookingDto, 2L);
        Thread.sleep(2000);
        itemService.createComment(commentDto, 2L, 1L);
        ItemDto item = itemService.getItem(1L, 1L);
        assertThat(item.getComments().size(), equalTo(1));
    }

    @Order(6)
    @Test
    void testGetWrongItem() {
        final ObjectNotFoundException exception = Assertions.assertThrows(
                ObjectNotFoundException.class, () -> itemService.getItem(99L, 1L));
        Assertions.assertEquals("Item with id not found99", exception.getMessage());
    }
}
