import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";

import EmptyState from "../../../components/common/EmptyState";
import ErrorState from "../../../components/common/ErrorState";
import LoadingState from "../../../components/common/LoadingState";

import { getApiErrorMessage } from "../../../utils/getApiErrorMessage";
import { queryKeys } from "../../../query/queryKeys";

import CartItem from "../components/CartItem";
import { deleteCartItem, getCart, updateCartItem } from "../api/cartApi";

import styles from "./CartPage.module.css";

function formatPrice(price) {
  return new Intl.NumberFormat("vi-VN").format(Number(price) || 0);
}

function CartPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [selectedItemIds, setSelectedItemIds] = useState([]);
  const [actionErrorMessage, setActionErrorMessage] = useState("");

  const {
    data: cartData = null,
    isLoading,
    error: cartError,
    refetch: refetchCart,
  } = useQuery({
    queryKey: queryKeys.cart.all,
    queryFn: getCart,
  });

  const updateCartMutation = useMutation({
    mutationFn: ({ cartItemId, quantity }) => updateCartItem(cartItemId, quantity),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.cart.all }),
  });

  const deleteCartMutation = useMutation({
    mutationFn: (cartItemId) => deleteCartItem(cartItemId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.cart.all }),
  });

  const errorMessage = cartError
    ? getApiErrorMessage(cartError, "Không thể tải giỏ hàng.")
    : actionErrorMessage;

  const updatingItemId = updateCartMutation.isPending
    ? updateCartMutation.variables?.cartItemId ?? null
    : null;

  const deletingItemId = deleteCartMutation.isPending
    ? deleteCartMutation.variables ?? null
    : null;

  const shopGroups = useMemo(() => {
    if (!cartData?.items) {
      return [];
    }

    const groups = new Map();

    cartData.items.forEach((item) => {
      if (!groups.has(item.shopId)) {
        groups.set(item.shopId, {
          shopId: item.shopId,
          shopName: item.shopName,
          items: [],
        });
      }

      groups.get(item.shopId).items.push(item);
    });

    return [...groups.values()];
  }, [cartData]);

  const availableItems = useMemo(
    () => cartData?.items?.filter((item) => item.available) || [],
    [cartData],
  );

  const availableItemIds = useMemo(
    () => new Set(availableItems.map((item) => item.id)),
    [availableItems],
  );

  const effectiveSelectedItemIds = useMemo(
    () => selectedItemIds.filter((id) => availableItemIds.has(id)),
    [availableItemIds, selectedItemIds],
  );

  const isAllSelected =
    availableItems.length > 0 &&
    effectiveSelectedItemIds.length === availableItems.length;

  const selectedItems = useMemo(() => {
    if (!cartData?.items) {
      return [];
    }

    return cartData.items.filter((item) => effectiveSelectedItemIds.includes(item.id));
  }, [cartData, effectiveSelectedItemIds]);

  const selectedTotal = useMemo(() => {
    return selectedItems.reduce(
      (total, item) => total + Number(item.lineTotal),
      0,
    );
  }, [selectedItems]);

  function loadCart() {
    setActionErrorMessage("");
    refetchCart();
  }

  function handleSelectItem(itemId) {
    setSelectedItemIds((currentIds) => {
      if (currentIds.includes(itemId)) {
        return currentIds.filter((id) => id !== itemId);
      }

      return [...currentIds, itemId];
    });
  }

  function handleSelectAll() {
    if (isAllSelected) {
      setSelectedItemIds([]);
      return;
    }

    setSelectedItemIds(availableItems.map((item) => item.id));
  }

  function handleSelectShop(shop) {
    const availableShopItemIds = shop.items
      .filter((item) => item.available)
      .map((item) => item.id);

    const allShopItemsSelected = availableShopItemIds.every((id) =>
      effectiveSelectedItemIds.includes(id),
    );

    setSelectedItemIds((currentIds) => {
      if (allShopItemsSelected) {
        return currentIds.filter(
          (id) => !availableShopItemIds.includes(id),
        );
      }

      return [...new Set([...currentIds, ...availableShopItemIds])];
    });
  }

  async function changeQuantity(item, quantity) {
    if (quantity < 1 || quantity > item.availableStock) {
      return;
    }

    setActionErrorMessage("");

    try {
      await updateCartMutation.mutateAsync({
        cartItemId: item.id,
        quantity,
      });
    } catch (error) {
      setActionErrorMessage(
        getApiErrorMessage(error, "Không thể cập nhật số lượng."),
      );
    }
  }

  function handleIncrease(item) {
    changeQuantity(item, item.quantity + 1);
  }

  function handleDecrease(item) {
    changeQuantity(item, item.quantity - 1);
  }

  async function handleDelete(item) {
    setActionErrorMessage("");

    try {
      await deleteCartMutation.mutateAsync(item.id);

      setSelectedItemIds((currentIds) =>
        currentIds.filter((id) => id !== item.id),
      );
    } catch (error) {
      setActionErrorMessage(
        getApiErrorMessage(error, "Không thể xóa sản phẩm."),
      );
    }
  }

  function handleCheckout() {
    if (effectiveSelectedItemIds.length === 0) {
      return;
    }

    navigate("/checkout", {
      state: {
        cartItemIds: effectiveSelectedItemIds,
      },
    });
  }

  if (isLoading && !cartData) {
    return (
      <LoadingState message="Đang tải giỏ hàng..." />
    );
  }

  if (errorMessage && !cartData) {
    return (
      <ErrorState
        message={errorMessage}
        onRetry={loadCart}
      />
    );
  }

  if (!cartData?.items?.length) {
    return (
      <EmptyState
        title="Giỏ hàng trống"
        description="Hãy thêm sản phẩm vào giỏ hàng."
      />
    );
  }

  return (
    <main className={styles.page}>
      <div className={styles.container}>
        <div className={styles.headerRow}>
          <div className={styles.productHeader}>
            <input
              type="checkbox"
              checked={isAllSelected}
              onChange={handleSelectAll}
            />

            <span>Sản Phẩm</span>
          </div>

          <span>Đơn Giá</span>
          <span>Số Lượng</span>
          <span>Số Tiền</span>
          <span>Thao Tác</span>
        </div>

        {shopGroups.map((shop) => {
          const availableShopItems =
            shop.items.filter(
              (item) => item.available,
            );

          const shopSelected =
            availableShopItems.length > 0 &&
            availableShopItems.every((item) =>
              effectiveSelectedItemIds.includes(item.id),
            );

          return (
            <section
              key={shop.shopId}
              className={styles.shopGroup}
            >
              <div className={styles.shopHeader}>
                <input
                  type="checkbox"
                  checked={shopSelected}
                  disabled={
                    availableShopItems.length === 0
                  }
                  onChange={() =>
                    handleSelectShop(shop)
                  }
                />

                <strong>{shop.shopName}</strong>

                <span className={styles.shopBadge}>
                  Shop
                </span>
              </div>

              {shop.items.map((item) => (
                <CartItem
                  key={item.id}
                  item={item}
                  checked={effectiveSelectedItemIds.includes(
                    item.id,
                  )}
                  isUpdating={
                    updatingItemId === item.id
                  }
                  isDeleting={
                    deletingItemId === item.id
                  }
                  onSelect={handleSelectItem}
                  onIncrease={handleIncrease}
                  onDecrease={handleDecrease}
                  onDelete={handleDelete}
                />
              ))}
            </section>
          );
        })}

        <div className={styles.checkoutBar}>
          <div className={styles.selectAllArea}>
            <input
              type="checkbox"
              checked={isAllSelected}
              onChange={handleSelectAll}
            />

            <button
              type="button"
              onClick={handleSelectAll}
            >
              Chọn Tất Cả ({availableItems.length})
            </button>
          </div>

          <div className={styles.checkoutSummary}>
            <div>
              Tổng thanh toán (
              {selectedItems.length} Sản phẩm):
              <strong>
                ₫{formatPrice(selectedTotal)}
              </strong>
            </div>

            <button
              type="button"
              className={styles.checkoutButton}
              disabled={
                effectiveSelectedItemIds.length === 0
              }
              onClick={handleCheckout}
            >
              Mua Hàng
            </button>
          </div>
        </div>
      </div>
    </main>
  );
}

export default CartPage;
