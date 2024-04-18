package io.agora.scene.eCommerce.shop

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.agora.rtc2.internal.CommonUtility.getSystemService
import io.agora.rtmsyncmanager.model.AUIRoomInfo
import io.agora.scene.base.manager.UserManager
import io.agora.scene.eCommerce.R
import io.agora.scene.eCommerce.databinding.CommerceShopGoodsItemLayoutBinding
import io.agora.scene.eCommerce.databinding.CommerceShopGoodsListDialogBinding
import io.agora.scene.eCommerce.service.GoodsModel
import io.agora.scene.eCommerce.service.ShowServiceProtocol
import io.agora.scene.eCommerce.service.ownerId
import io.agora.scene.widget.basic.BindingSingleAdapter
import io.agora.scene.widget.basic.BindingViewHolder

class GoodsListDialog constructor(
    context: Context,
    private val roomId: String
) : BottomSheetDialog(context, R.style.commerce_alert_dialog) {

    private val tag = "GoodsListDialog"

    private val binding by lazy { CommerceShopGoodsListDialogBinding.inflate(LayoutInflater.from(context)) }

    private var isRoomOwner = false

    private val mService by lazy { ShowServiceProtocol.getImplInstance() }

    private val dataSource = arrayListOf<GoodsModel>()

    private lateinit var mAdapter: ShopAdapter

    init {
        setContentView(binding.root)
        setupView()
        setupShop()
    }

    private fun setupShop() {
        mService.shopSubscribe(roomId) { models ->
            updateList(models)
        }
    }

    private fun updateList(goodsModels: List<GoodsModel>) {
        if (dataSource.isEmpty()) {
            for (model in goodsModels) {
                if (model.imageName.contains("0")) {
                    model.picResource = R.drawable.commerce_shop_goods_0
                } else if (model.imageName.contains("1")) {
                    model.picResource = R.drawable.commerce_shop_goods_1
                } else {
                    model.picResource = R.drawable.commerce_shop_goods_2
                }
                dataSource.add(model)
            }
        } else {
            for ((index, model) in goodsModels.withIndex()) {
                val item = dataSource[index]
                item.quantity = model.quantity
            }
        }
        mAdapter.resetAll(dataSource)
    }

    private fun setupView() {
        val roomInfo = mService.getRoomInfo(roomId) ?: AUIRoomInfo()
        isRoomOwner = roomInfo.ownerId == UserManager.getInstance().user.id.toInt()
        mAdapter = ShopAdapter(isRoomOwner)
        binding.recyclerView.adapter = mAdapter
        mAdapter.onClickBuy = { goodsId ->
            mService.shopBuyItem(roomId, goodsId) { e ->
                if (e != null) { // bought result
                    ShoppingResultDialog(context, context.getString(R.string.commerce_shop_alert_bought)).show()
                } else {
                    ShoppingResultDialog(context, context.getString(R.string.commerce_shop_alert_sold_out)).show()
                }
            }
        }
        mAdapter.onUserChangedQty = { goodsId, qty ->
            mService.shopUpdateItem(roomId, goodsId, qty)
        }
    }

    private class ShopAdapter(
        private val isRoomOwner: Boolean
    ): BindingSingleAdapter<GoodsModel, CommerceShopGoodsItemLayoutBinding>(){

        var onClickBuy: ((goodsId: String) -> Unit)? = null

        var onUserChangedQty: ((goodsId: String, qty: Int) -> Unit)? = null

        override fun onBindViewHolder(
            holder: BindingViewHolder<CommerceShopGoodsItemLayoutBinding>,
            position: Int
        ) {
            getItem(position)?.let { item ->
                holder.binding.tvItemName.text = item.title
                holder.binding.ivCommodity.setImageResource(item.picResource)
                holder.binding.tvPrice.text = String.format("$%.0f", item.price)
                val context = holder.itemView.context
                holder.binding.tvQty.text = context.getString(R.string.commerce_shop_item_qty, item.quantity.toString())
                if (item.quantity == 0) {
                    holder.binding.btnBuy.setBackgroundResource(R.drawable.commerce_corner_radius_gray)
                    holder.binding.btnBuy.text = context.getString(R.string.commerce_shop_item_sold_out)
                    holder.binding.btnBuy.setTextColor(Color.parseColor("#A5ADBA"))
                    holder.binding.btnBuy.isEnabled = false
                } else {
                    holder.binding.btnBuy.setBackgroundResource(R.drawable.commerce_corner_radius_gradient_orange)
                    holder.binding.btnBuy.text = context.getString(R.string.commerce_shop_auction_buy)
                    holder.binding.btnBuy.setTextColor(Color.parseColor("#191919"))
                    holder.binding.btnBuy.isEnabled = true
                }
                if (isRoomOwner) {
                    holder.binding.btnBuy.visibility = View.GONE
                    holder.binding.llStepper.visibility = View.VISIBLE
                    holder.binding.etQty.setText(item.quantity.toString())
                    holder.binding.btnAdd.setOnClickListener {
                        val value = (holder.binding.etQty.text ?: "0").toString().toInt()
                        val newValue = fitValue(value + 1)
                        holder.binding.etQty.setText(newValue.toString())
                        onUserChangedQty?.invoke(item.goodsId, newValue)
                    }
                    holder.binding.btnReduce.setOnClickListener {
                        val value = (holder.binding.etQty.text ?: "0").toString().toInt()
                        val newValue = fitValue(value - 1)
                        holder.binding.etQty.setText(newValue.toString())
                        onUserChangedQty?.invoke(item.goodsId, newValue)
                    }
                    holder.binding.etQty.setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            val inputMethodManager = getSystemService(context, Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            inputMethodManager.hideSoftInputFromWindow(holder.binding.etQty.windowToken, 0)
                            holder.binding.etQty.clearFocus()

                            val qty = holder.binding.etQty.text.toString().toIntOrNull() ?: 0
                            val newValue = fitValue(qty)
                            onUserChangedQty?.invoke(item.goodsId, newValue)
                            return@setOnEditorActionListener true
                        }
                        false
                    }
                } else {
                    holder.binding.btnBuy.visibility = View.VISIBLE
                    holder.binding.llStepper.visibility = View.GONE
                    holder.binding.btnBuy.setOnClickListener {
                        onClickBuy?.invoke(item.goodsId)
                    }
                }
            }
        }

        private fun fitValue(value: Int): Int {
            if (value < 0) { return 0 }
            if (value > 99) { return 99 }
            return value
        }
    }
}