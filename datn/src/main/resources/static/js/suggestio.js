document.addEventListener("DOMContentLoaded", function () {
    const input = document.querySelector("input[name='keyword']");
    const suggestionBox = document.getElementById("suggestion-box");
    const form = document.getElementById("search-form");

    input.addEventListener("input", debounce(function () {
        const query = input.value.trim();
        if (query.length < 2) {
            suggestionBox.style.display = "none";
            suggestionBox.innerHTML = "";
            return;
        }

        fetch(`/products/search-suggestions?query=` + encodeURIComponent(query))
            .then(res => res.json())
            .then(data => {
                if (data.length === 0) {
                    suggestionBox.style.display = "none";
                    suggestionBox.innerHTML = "";
                    return;
                }
                // Hiển thị suggestion box với dữ liệu trả về
                suggestionBox.style.display = "block";
                suggestionBox.innerHTML = data.map(item => `
                    <div class="px-4 py-2 hover:bg-yellow-100 cursor-pointer suggestion-item" data-value="${item}">
                        ${item}
                    </div>
                `).join("");

                suggestionBox.style.display = "block";
            });
    }, 300)); // Debounce 300ms

    // Sử dụng event delegation thay vì addEventListener cho từng item
    suggestionBox.addEventListener("click", function (e) {
        const item = e.target.closest('.suggestion-item');
        if (item) {
            e.preventDefault();
            e.stopPropagation();
            
            input.value = item.textContent.trim();
            suggestionBox.style.display = "none";
            
            // Delay submit để đảm bảo value đã được cập nhật
            setTimeout(() => {
                form.submit();
            }, 50);
        }
    });

    // Đóng suggestion box khi click ra ngoài
    document.addEventListener("click", function (e) {
        if (!form.contains(e.target)) {
            suggestionBox.style.display = "none";
        }
    });

    // Giữ suggestion box khi click vào input
    input.addEventListener("click", function (e) {
        if (suggestionBox.innerHTML.trim() !== "") {
            suggestionBox.style.display = "block";
        }
    });
});

// Hàm debounce để giảm số lần gọi API
function debounce(func, timeout = 300) {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => { func.apply(this, args); }, timeout);
    };
}