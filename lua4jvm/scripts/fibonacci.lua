function fibonacci(m)
	if m < 2 then
		return m
	end
	return fibonacci(m - 1) + fibonacci(m - 2)
end

n = tonumber(arg[1])
print(fibonacci(n))