package com.example.backend.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.backend.domain.MenuCategory;
import com.example.backend.domain.MenuItem;
import com.example.backend.repository.MenuCategoryRepository;
import com.example.backend.repository.MenuItemRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDemoData(MenuCategoryRepository categoryRepository, MenuItemRepository menuItemRepository) {
        return args -> {
            if (categoryRepository.count() > 0 || menuItemRepository.count() > 0) {
                return;
            }

            MenuCategory hot = new MenuCategory();
            hot.setName("热销");
            hot.setSortOrder(1);
            hot.setActive(true);

            MenuCategory staple = new MenuCategory();
            staple.setName("主食");
            staple.setSortOrder(2);
            staple.setActive(true);

            MenuCategory drink = new MenuCategory();
            drink.setName("饮品");
            drink.setSortOrder(3);
            drink.setActive(true);

            List<MenuCategory> categories = categoryRepository.saveAll(List.of(hot, staple, drink));
            MenuCategory hotCategory = categories.get(0);
            MenuCategory stapleCategory = categories.get(1);
            MenuCategory drinkCategory = categories.get(2);

            menuItemRepository.saveAll(List.of(
                    createItem(hotCategory, "招牌红烧牛肉饭", "牛肉软烂入味，配时蔬与米饭", "36.00"),
                    createItem(hotCategory, "香辣鸡腿堡套餐", "鸡腿堡+薯条+可乐", "32.00"),
                    createItem(stapleCategory, "菌菇鸡汤面", "手工拉面，搭配鲜香鸡汤", "28.00"),
                    createItem(stapleCategory, "扬州炒饭", "火候十足，颗粒分明", "22.00"),
                    createItem(drinkCategory, "鲜榨橙汁", "不加糖，现点现榨", "16.00"),
                    createItem(drinkCategory, "柠檬蜂蜜水", "酸甜清爽", "12.00")
            ));
        };
    }

    private MenuItem createItem(MenuCategory category, String name, String description, String price) {
        MenuItem item = new MenuItem();
        item.setCategory(category);
        item.setName(name);
        item.setDescription(description);
        item.setPrice(new BigDecimal(price));
        item.setAvailable(true);
        return item;
    }
}
