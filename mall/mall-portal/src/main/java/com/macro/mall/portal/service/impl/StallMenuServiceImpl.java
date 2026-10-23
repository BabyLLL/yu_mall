package com.macro.mall.portal.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.macro.mall.mapper.PmsProductMapper;
import com.macro.mall.mapper.PmsSkuStockMapper;
import com.macro.mall.model.PmsProduct;
import com.macro.mall.model.PmsProductExample;
import com.macro.mall.model.PmsSkuStock;
import com.macro.mall.model.PmsSkuStockExample;
import com.macro.mall.portal.service.StallMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 摊位菜单Service实现
 * Created by macro on 2026/10/12.
 */
@Service
public class StallMenuServiceImpl implements StallMenuService {

    @Autowired
    private PmsProductMapper productMapper;
    @Autowired
    private PmsSkuStockMapper skuStockMapper;

    @Override
    public List<PmsProduct> listMenu() {
        PmsProductExample example = new PmsProductExample();
        example.createCriteria().andPublishStatusEqualTo(1).andDeleteStatusEqualTo(0);
        example.setOrderByClause("sort desc, id desc");
        List<PmsProduct> productList = productMapper.selectByExample(example);
        //真实剩余库存（stock-lock_stock）覆写返回值：售罄商品不再过滤，由前端以售罄样式展示
        List<PmsProduct> result = new ArrayList<>();
        for (PmsProduct product : productList) {
            product.setStock(realStock(product));
            result.add(product);
        }
        return result;
    }

    private int realStock(PmsProduct product) {
        PmsSkuStockExample skuExample = new PmsSkuStockExample();
        skuExample.createCriteria().andProductIdEqualTo(product.getId());
        List<PmsSkuStock> skuList = skuStockMapper.selectByExample(skuExample);
        if (CollUtil.isEmpty(skuList)) {
            //无SKU时按商品库存
            return product.getStock() == null ? 0 : product.getStock();
        }
        //摊位商品取第一个SKU的真实库存
        PmsSkuStock sku = skuList.get(0);
        return sku.getStock() - sku.getLockStock();
    }
}
