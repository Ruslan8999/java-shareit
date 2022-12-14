package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exceptions.BadRequestException;
import ru.practicum.shareit.exceptions.ObjectNotFoundException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Override
    public Collection<ItemDto> getAllItems(Long userId) {
        return itemRepository.findAllByOwnerId(userId).stream()
                .map(ItemMapper::toItemDto)
                .map(this::addBookingsToItem)
                .map(this::addCommentsToItem)
                .sorted(Comparator.comparingLong(ItemDto::getId))
                .collect(Collectors.toList());
    }

    @Override
    public ItemDto getItem(Long id, Long userId) {
        Optional<Item> item = itemRepository.findById(id);
        if (item.isPresent()) {
            ItemDto itemDto = ItemMapper.toItemDto(item.get());
            if (item.get().getOwner().getId().equals(userId)) {
                addBookingsToItem(itemDto);
            }
            addCommentsToItem(itemDto);
            return itemDto;
        } else {
            throw new ObjectNotFoundException("Item with id not found" + id);
        }
    }

    private ItemDto addBookingsToItem(ItemDto dto) {
        Item item = new Item();
        ItemMapper.toItem(item, dto);
        var lastBookings = bookingRepository.findTop1ByItemAndEndIsBeforeOrderByEndDesc(item, LocalDateTime.now());
        if (lastBookings != null) {
            dto.setLastBooking(BookingMapper.toBookingDto(lastBookings));
        }
        var nextBookings = bookingRepository.findTop1ByItemAndStartIsAfterOrderByStartDesc(item, LocalDateTime.now());
        if (nextBookings != null) {
            dto.setNextBooking(BookingMapper.toBookingDto(nextBookings));
        }
        return dto;
    }

    private ItemDto addCommentsToItem(ItemDto dto) {
        Item item = new Item();
        ItemMapper.toItem(item, dto);
        var comments = commentRepository.findAllByItem(item);
        if (comments != null) {
            dto.setComments(comments.stream()
                    .map(CommentMapper::toCommentDto)
                    .collect(Collectors.toList()));
        } else {
            dto.setComments(new ArrayList<>());
        }
        return dto;
    }

    @Override
    public ItemDto addItem(Long userId, ItemDto itemDto) {
        Item newItem = ItemMapper.toItem(new Item(), itemDto);
        Optional<User> owner = userRepository.findById(userId);
        if (owner.isEmpty()) {
            throw new ObjectNotFoundException(String.format("User id = %d not exist", userId));
        } else {
            newItem.setOwner(owner.get());
        }
        return ItemMapper.toItemDto(itemRepository.save(newItem));
    }

    @Override
    public ItemDto editItem(Long userId, Long itemId, ItemDto itemDto) {
        Item item = new Item(itemRepository.getReferenceById(itemId));
        if (!Objects.equals(item.getOwner().getId(), userId)) {
            throw new ObjectNotFoundException("User is not owner of item!");
        }
        Item editItem = itemRepository.save(ItemMapper.toItem(item, itemDto));
        log.info("Item updated" + editItem);
        return ItemMapper.toItemDto(editItem);
    }

    @Override
    public Collection<ItemDto> searchItemByText(String text) {
        if (text.isBlank()) {
            return new ArrayList<>();
        }
        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    @Override
    public CommentDto createComment(CommentDto commentDto, Long userId, Long itemId) {
        Comment newComment = new Comment();
        CommentMapper.toComment(newComment, commentDto);
        newComment.setCreated(LocalDateTime.now());
        var owner = userRepository.findById(userId);
        if (owner.isEmpty()) {
            throw new ObjectNotFoundException(String.format("User with id=%d not found", userId));
        } else {
            newComment.setAuthor(owner.get());
        }
        var item = itemRepository.findById(itemId);
        if (item.isEmpty()) {
            throw new ObjectNotFoundException(String.format("Item with id=%d not found", userId));
        } else {
            newComment.setItem(item.get());
        }
        Booking booking = bookingRepository.findTop1ByItemAndBookerAndEndIsBefore(item.get(), owner.get(), LocalDateTime.now());
        if (booking == null) {
            throw new BadRequestException(String.format("User with id=%d not owner of item id=%d", userId, itemId));
        }
        Comment createdComment = commentRepository.save(newComment);
        log.info("Comment created" + createdComment);
        return CommentMapper.toCommentDto(createdComment);
    }
}
