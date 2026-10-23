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
        //过滤售罄商品：真实库存（stock-lock_stock）<=0 视为售罄
        List<PmsProduct> result = new ArrayList<>();
        for (PmsProduct product : productList) {
            if (isSoldOut(product)) {
                continue;
            }
            result.add(product);
        }
        return result;
    }

    private boolean isSoldOut(PmsProduct product) {
        PmsSkuStockExample skuExample = new PmsSkuStockExample();
        skuExample.createCriteria().andProductIdEqualTo(product.getId());
        List<PmsSkuStock> skuList = skuStockMapper.selectByExample(skuExample);
        if (CollUtil.isEmpty(skuList)) {
            //无SKU时按商品库存判断
            return product.getStock() == null || product.getStock() <= 0;
        }
        //摊位商品取第一个SKU的真实库存
        PmsSkuStock sku = skuList.get(0);
        return sku.getStock() - sku.getLockStock() <= 0;
    }
}
