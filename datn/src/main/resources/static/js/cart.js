function addToCart(button) {
  const variantId = button.getAttribute("data-variant-id");

  fetch("/api/cart/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: `variantId=${variantId}&quantity=1`,
  })
    .then((response) => {
      if (!response.ok) {
        return response.text().then((text) => {
          throw new Error(text);
        });
      }
      return response.text(); // OK
    })
    .then((data) => {
      alert("✅ Đã thêm sản phẩm vào giỏ hàng");
      updateCartCount();
      updateMiniCart();
    })
    .catch((error) => {
      alert("❌ Lỗi: " + error.message);
    });
}

function updateCartCount() {
  fetch("/api/cart/count")
    .then((response) => response.json())
    .then((count) => {
      const el = document.querySelector(".cart-count");
      if (el) el.textContent = count;
    });
}

function updateMiniCart() {
  fetch("/api/cart/items")
    .then((response) => {
      if (response.status === 401) {
        console.log("Chưa đăng nhập - không tải giỏ hàng");
        return [];
      }
      return response.json();
    })
    .then((items) => {
      if (!Array.isArray(items)) return;

      const container = document.querySelector(".group .absolute.z-50.p-4");
      if (!container) return;

      container.innerHTML = "";

      if (items.length === 0) {
        container.innerHTML = `
          <div class="text-center text-gray-500 text-sm py-4">Chưa có sản phẩm</div>
        `;
        return;
      }

      let total = 0;

      items.forEach((item) => {
        const imageUrl = item.image
          ? `/images/${item.image}`
          : "https://via.placeholder.com/60x60?text=No+Image";
        const itemTotal = item.price * item.quantity;
        total += itemTotal;

        container.innerHTML += `
          <div class="flex gap-3 border-b pb-3 mb-3" data-variant-id="${
            item.variantId
          }">
            <img src="${imageUrl}" class="w-12 h-12 object-cover rounded" />
            <div class="flex-1">
              <div class="flex justify-between">
                <p class="font-semibold text-sm text-black">
                  ${item.productName} ${item.storage || ""} - ${
          item.color || ""
        }
                </p>
                <button
                  onclick="removeFromCart(${item.variantId})"
                  class="text-gray-400 hover:text-red-600 focus:outline-none"
                  type="button"
                >Xóa</button>
              </div>
              
              <div class="flex justify-between items-center mt-2">
                <div class="flex items-center border rounded">
                  <button class="px-2 text-gray-600 hover:bg-gray-100" onclick="updateQuantity(${
                    item.variantId
                  }, ${item.quantity - 1})">-</button>
                  <span class="px-2 text-black" id="qty-${item.variantId}">${
          item.quantity
        }</span>
                  <button class="px-2 text-gray-600 hover:bg-gray-100" onclick="updateQuantity(${
                    item.variantId
                  }, ${item.quantity + 1})">+</button>
                </div>
                <div class="text-red-600 font-bold text-right text-sm" id="item-total-${
                  item.variantId
                }">
                  ${formatCurrency(itemTotal)}
                </div>
              </div>
            </div>
          </div>
        `;
      });

      container.innerHTML += `
        <div class="flex justify-between font-semibold text-base mt-4 mb-4">
          <span class="text-black">Tổng tiền:</span>
          <span class="text-red-600">${formatCurrency(total)}</span>
        </div>
        <a href="/checkout" class="block w-full text-center bg-blue-600 text-white py-2 rounded hover:bg-blue-700 transition">
          Thanh toán
        </a>
      `;
    });
}
function formatCurrency(number) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(number);
}

document.addEventListener("DOMContentLoaded", function () {
  updateCartCount();
  updateMiniCart();
});

function removeFromCart(variantId) {
  if (!confirm("Bạn có chắc muốn xóa sản phẩm này không?")) return;

  fetch("/api/cart/remove", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: `variantId=${variantId}`,
  })
    .then((res) => {
      if (!res.ok) throw new Error("Lỗi khi xóa sản phẩm");
      return res.text();
    })
    .then(() => {
      updateMiniCart();
      updateCartCount();
    })
    .catch((err) => alert("❌ " + err.message));
}

function updateQuantity(variantId, newQty) {
  fetch("/api/cart/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: `variantId=${variantId}&quantity=${newQty}`,
  })
    .then((res) => {
      if (!res.ok) throw new Error("Không thể cập nhật số lượng");
      return res.text();
    })
    .then(() => {
      updateMiniCart();
      updateCartCount();
    })
    .catch((err) => alert("❌ " + err.message));
}

