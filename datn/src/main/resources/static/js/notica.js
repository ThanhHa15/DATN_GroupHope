  document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll(".wishlist-toggle").forEach(btn => {
      btn.addEventListener("click", function () {
        const variantId = this.getAttribute("data-variant-id");
        fetch("/add", {
          method: "POST",
          headers: {
            "Content-Type": "application/x-www-form-urlencoded"
          },
          body: new URLSearchParams({ variantId })
        })
          .then(response => {
            if (!response.ok) throw new Error("Lỗi server");
            return response.json(); // yêu cầu backend trả JSON
          })
          .then(data => {
            // Cập nhật icon
            const icon = this.querySelector("i");
            if (data.status === "added") {
              icon.className = "ri-heart-fill text-red-600";
            } else if (data.status === "removed") {
              icon.className = "ri-heart-line text-gray-600 hover:text-red-500";
            }

            // Hiển thị thông báo
            showNotification(data.message, "success");
          })
          .catch(err => {
            showNotification("Vui lòng đăng nhập!", "error");
          });
      });
    });
  });